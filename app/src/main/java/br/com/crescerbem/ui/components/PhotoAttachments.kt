package br.com.crescerbem.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.crescerbem.data.repo.PhotoRepository
import br.com.crescerbem.photos.CameraTarget
import br.com.crescerbem.photos.PhotoStorage
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fotos de um formulário ainda não salvo. Arquivos novos ficam pendentes até [commit];
 * se o formulário for abandonado, [discardUncommitted] apaga o que foi adicionado.
 */
class PhotoDraft(
    private val repository: PhotoRepository,
    private val storage: PhotoStorage,
    private val scope: CoroutineScope,
) {
    private val _files = MutableStateFlow<List<String>>(emptyList())
    val files: StateFlow<List<String>> = _files.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val addedThisSession = mutableSetOf<String>()
    private var committed = false

    fun load(fileNames: List<String>) {
        _files.value = fileNames
    }

    fun fileFor(fileName: String): File = storage.fileFor(fileName)

    fun prepareCamera(): CameraTarget = storage.createCameraTarget()

    fun onCameraResult(targetName: String, success: Boolean) {
        scope.launch {
            _busy.value = true
            try {
                storage.finishCameraCapture(targetName, success)?.let(::add)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Não foi possível salvar a foto. Tente de novo."
            } finally {
                _busy.value = false
            }
        }
    }

    fun importFromGallery(uris: List<Uri>) {
        scope.launch {
            _busy.value = true
            try {
                uris.forEach { uri ->
                    try {
                        add(storage.importFromUri(uri))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _message.value = "Uma das fotos não pôde ser adicionada."
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }

    private fun add(fileName: String) {
        addedThisSession += fileName
        _files.update { it + fileName }
    }

    fun remove(fileName: String) {
        _files.update { it - fileName }
        // Foto nova: apaga já. Foto já salva: só sai ao salvar o formulário.
        if (addedThisSession.remove(fileName)) repository.discardFile(fileName)
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun commit(babyId: Long, type: br.com.crescerbem.data.model.RecordType, ownerId: Long) {
        repository.sync(babyId, type, ownerId, _files.value)
        committed = true
        addedThisSession.clear()
    }

    fun discardUncommitted() {
        if (!committed) addedThisSession.forEach(repository::discardFile)
        addedThisSession.clear()
    }
}

private const val MAX_GALLERY_PICK = 6

/** Botões "Tirar foto" e "Galeria" com miniaturas removíveis. */
@Composable
fun PhotoAttachmentSection(
    draft: PhotoDraft,
    modifier: Modifier = Modifier,
    title: String = "Fotos",
    hint: String? = null,
) {
    val files by draft.files.collectAsStateWithLifecycle()
    val busy by draft.busy.collectAsStateWithLifecycle()
    val message by draft.message.collectAsStateWithLifecycle()
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pendingCamera?.let { draft.onCameraResult(it, ok) }
        pendingCamera = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICK),
    ) { uris -> if (uris.isNotEmpty()) draft.importFromGallery(uris) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldTitle(title)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    draft.clearMessage()
                    val target = draft.prepareCamera()
                    pendingCamera = target.fileName
                    try {
                        cameraLauncher.launch(target.uri)
                    } catch (e: ActivityNotFoundException) {
                        pendingCamera = null
                        draft.onCameraResult(target.fileName, false)
                        draft.showMessage("Nenhum aplicativo de câmera encontrado.")
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tirar foto")
            }
            OutlinedButton(
                onClick = {
                    draft.clearMessage()
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galeria")
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (files.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(files, key = { _, name -> name }) { index, name ->
                    // Remover fica abaixo da miniatura, longe da área que amplia a foto.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = draft.fileFor(name),
                            contentDescription = "Foto ${index + 1}. Toque para ampliar.",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { preview = name },
                        )
                        TextButton(
                            onClick = { draft.remove(name) },
                            modifier = Modifier.semantics { contentDescription = "Remover foto ${index + 1}" },
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remover")
                        }
                    }
                }
            }
        }
    }

    preview?.let { name -> PhotoPreviewDialog(draft.fileFor(name)) { preview = null } }
}

@Composable
fun PhotoPreviewDialog(file: File, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        text = {
            AsyncImage(
                model = file,
                contentDescription = "Foto ampliada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}
