package br.com.crescerbem.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.RecordType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class FormStatus(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
)

/** Resultado de um salvamento: a qual bebê e a qual registro as fotos pertencem. */
data class SavedRef(val babyId: Long, val ownerId: Long)

/**
 * Base dos formulários: resolve o bebê, carrega o registro em edição, salva, exclui e
 * cuida das fotos (vinculadas só ao salvar; descartadas se o formulário for abandonado).
 */
abstract class BaseFormViewModel(
    protected val container: AppContainer,
    private val recordType: RecordType,
    protected val recordId: Long?,
    private val requiresBaby: Boolean = true,
) : ViewModel() {
    val photos = PhotoDraft(container.photoRepository, container.photoStorage, viewModelScope)

    private val _status = MutableStateFlow(FormStatus())
    val status: StateFlow<FormStatus> = _status.asStateFlow()

    protected var babyId: Long? = null
        private set

    val isEdit: Boolean get() = recordId != null

    /** [load] busca o registro em edição, preenche o formulário e devolve o id do bebê dono. */
    protected fun start(load: suspend (Long) -> Long?) {
        viewModelScope.launch {
            try {
                babyId = if (recordId != null) load(recordId) else container.babyRepository.activeBaby.first()?.id
                if (recordId != null && babyId != null) {
                    photos.load(container.photoRepository.fileNamesFor(recordType, recordId))
                }
                val problem = when {
                    recordId != null && babyId == null -> "Registro não encontrado."
                    requiresBaby && babyId == null -> "Cadastre um bebê antes de registrar."
                    else -> null
                }
                _status.update { it.copy(loading = false, error = problem) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.update { it.copy(loading = false, error = "Não foi possível carregar os dados.") }
            }
        }
    }

    /** Salva com [write] e vincula as fotos ao registro salvo. */
    protected fun persist(write: suspend (babyId: Long?) -> SavedRef) {
        val current = _status.value
        if (current.saving || current.loading) return
        if (requiresBaby && babyId == null) {
            showError("Cadastre um bebê antes de registrar.")
            return
        }
        _status.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val ref = write(babyId)
                photos.commit(ref.babyId, recordType, ref.ownerId)
                _status.update { it.copy(saving = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.update { it.copy(saving = false, error = "Não foi possível salvar. Tente de novo.") }
            }
        }
    }

    /** Atalho para registros comuns, que sempre pertencem a um bebê. */
    protected fun persistRecord(write: suspend (babyId: Long) -> Long) = persist { id ->
        val owner = requireNotNull(id)
        SavedRef(owner, write(owner))
    }

    open fun delete() {
        val id = recordId ?: return
        viewModelScope.launch {
            try {
                container.recordRepository.delete(recordType, id)
                _status.update { it.copy(done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Não foi possível excluir. Tente de novo.")
            }
        }
    }

    protected fun showError(message: String) {
        _status.update { it.copy(error = message) }
    }

    fun clearError() {
        _status.update { it.copy(error = null) }
    }

    override fun onCleared() {
        photos.discardUncommitted()
        super.onCleared()
    }
}

/** Hora inicial de um registro novo: agora, se for hoje; meio-dia para outros dias. */
fun defaultDateTime(date: LocalDate?): LocalDateTime {
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    return if (date == null || date == now.toLocalDate()) now else date.atTime(LocalTime.NOON)
}

/** Converte texto opcional: vazio vira nulo. */
fun String.orNullIfBlank(): String? = trim().takeIf { it.isNotEmpty() }
