package br.com.crescerbem.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import br.com.crescerbem.data.repo.PhotoFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Destino temporário para a câmera do sistema gravar a foto. */
data class CameraTarget(val fileName: String, val uri: Uri)

/**
 * Fotos ficam em files/photos (área privada, fora da galeria). Ao importar, a imagem é
 * reduzida, girada conforme o EXIF e regravada em JPEG: isso economiza espaço e descarta
 * metadados como localização (ver docs/privacidade.md).
 */
class PhotoStorage(context: Context) : PhotoFiles {
    private val appContext = context.applicationContext
    private val dir: File = File(appContext.filesDir, DIR).apply { mkdirs() }

    init {
        removeStaleCameraFiles(dir, System.currentTimeMillis())
    }

    fun fileFor(fileName: String): File = File(dir, fileName)

    /** Cria um arquivo vazio e a Uri (FileProvider) para a câmera gravar nele. */
    fun createCameraTarget(): CameraTarget {
        dir.mkdirs()
        val name = "$CAMERA_PREFIX${UUID.randomUUID()}.jpg"
        val file = File(dir, name)
        file.createNewFile()
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        return CameraTarget(name, uri)
    }

    /** Processa a foto tirada pela câmera. Retorna o nome final ou nulo se a captura falhou. */
    suspend fun finishCameraCapture(target: String, success: Boolean): String? = withContext(Dispatchers.IO) {
        val raw = File(dir, target)
        try {
            if (!success || !raw.exists() || raw.length() == 0L) return@withContext null
            importFile(raw)
        } finally {
            raw.delete()
        }
    }

    /** Copia uma imagem escolhida na galeria (Photo Picker) para a pasta privada. */
    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Não foi possível abrir a imagem.")
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Não foi possível ler a imagem.")
        val orientation = resolver.openInputStream(uri)?.use { readOrientation(ExifInterface(it)) }
            ?: ExifInterface.ORIENTATION_NORMAL
        save(bitmap, orientation)
    }

    private fun importFile(file: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: error("Não foi possível ler a foto.")
        return save(bitmap, readOrientation(ExifInterface(file.path)))
    }

    private fun readOrientation(exif: ExifInterface): Int =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    private fun save(source: Bitmap, orientation: Int): String {
        val rotated = rotate(source, orientation)
        val scale = ImageSizing.scaleFactor(rotated.width, rotated.height, MAX_SIDE_PX)
        val output = if (scale < 1f) {
            rotated.scale(
                (rotated.width * scale).toInt().coerceAtLeast(1),
                (rotated.height * scale).toInt().coerceAtLeast(1),
            )
        } else {
            rotated
        }
        val name = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(dir, name)).use { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return name
    }

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun delete(fileName: String) {
        // Evita apagar fora da pasta de fotos caso um nome inválido chegue aqui.
        val file = File(dir, fileName)
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }

    override fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val DIR = "photos"
        const val MAX_SIDE_PX = 1600
        const val JPEG_QUALITY = 85
        const val CAMERA_PREFIX = "camera_"

        /** Idade mínima para considerar órfão um arquivo de câmera (captura que nunca voltou). */
        const val STALE_CAMERA_MILLIS = 24L * 60 * 60 * 1000

        /**
         * Apaga arquivos temporários de câmera antigos, que sobram se o sistema encerrar o app
         * durante a captura. Arquivos recentes ficam: podem ser uma captura em andamento.
         * Retorna quantos arquivos foram apagados.
         */
        fun removeStaleCameraFiles(dir: File, nowMillis: Long, maxAgeMillis: Long = STALE_CAMERA_MILLIS): Int =
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith(CAMERA_PREFIX) && nowMillis - it.lastModified() > maxAgeMillis }
                ?.count { it.delete() }
                ?: 0
    }
}

/** Cálculos de redimensionamento, separados para teste. */
object ImageSizing {
    /** Maior potência de 2 que mantém o lado maior acima de [maxSide] depois da amostragem. */
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    /** Fator para o lado maior caber em [maxSide] (1 se já cabe). */
    fun scaleFactor(width: Int, height: Int, maxSide: Int): Float {
        val longest = maxOf(width, height)
        return if (longest <= maxSide || longest == 0) 1f else maxSide.toFloat() / longest
    }
}
