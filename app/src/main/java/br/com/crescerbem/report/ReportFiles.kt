package br.com.crescerbem.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.crescerbem.domain.Formats
import java.io.File
import java.time.LocalDate

/** PDFs ficam em files/reports e só saem do aparelho pela folha de compartilhamento. */
class ReportFiles(context: Context) {
    private val appContext = context.applicationContext
    private val dir: File = File(appContext.filesDir, DIR)

    fun fileFor(start: LocalDate, end: LocalDate): File {
        dir.mkdirs()
        return File(dir, "relatorio-crescer-bem-${Formats.fileStamp(start)}-a-${Formats.fileStamp(end)}.pdf")
    }

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    fun shareIntent(file: File): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Crescer Bem")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Compartilhar relatório")
    }

    fun viewIntent(file: File): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriFor(file), MIME)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Mantém só o relatório mais recente (coleta mínima: PDFs antigos não se acumulam). */
    fun keepOnly(file: File) = deleteOthers(dir, file)

    companion object {
        private const val DIR = "reports"
        private const val MIME = "application/pdf"

        /** Apaga todos os arquivos de [dir] exceto [keep]. Retorna quantos foram apagados. */
        fun deleteOthers(dir: File, keep: File): Int =
            dir.listFiles()
                ?.filter { it.isFile && it.canonicalPath != keep.canonicalPath }
                ?.count { it.delete() }
                ?: 0
    }
}
