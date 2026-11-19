package br.com.crescerbem.ui.screens.moments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.PhotoAttachmentSection
import br.com.crescerbem.ui.components.SuggestionChips
import br.com.crescerbem.ui.components.TextInput
import br.com.crescerbem.ui.components.TimeField
import br.com.crescerbem.ui.components.defaultDateTime
import br.com.crescerbem.ui.components.orNullIfBlank
import br.com.crescerbem.ui.navigation.FormArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

private val MOMENT_SUGGESTIONS = listOf(
    "Primeiro banho",
    "Primeiro sorriso",
    "Firmou o pescoço",
    "Rolou sozinho",
    "Sentou sem apoio",
    "Primeiro dentinho",
    "Engatinhou",
    "Primeiros passos",
    "Primeira palavra",
    "Visita especial",
)

data class MomentForm(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val description: String = "",
    val titleError: String? = null,
)

class MomentFormViewModel(container: AppContainer, id: Long?, presetDate: LocalDate?) :
    BaseFormViewModel(container, RecordType.MOMENT, id) {

    private val _form = MutableStateFlow(
        defaultDateTime(presetDate).let { MomentForm(date = it.toLocalDate(), time = it.toLocalTime()) },
    )
    val form: StateFlow<MomentForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getMoment(recordId)?.let { m ->
                _form.value = MomentForm(
                    title = m.title,
                    date = m.dateTime.toLocalDate(),
                    time = m.dateTime.toLocalTime(),
                    description = m.description.orEmpty(),
                )
                m.babyId
            }
        }
    }

    fun update(transform: (MomentForm) -> MomentForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        if (f.title.isBlank()) {
            _form.update { it.copy(titleError = "Dê um título ao momento.") }
            return
        }
        persistRecord { babyId ->
            container.recordRepository.saveMoment(
                Moment(
                    id = recordId ?: 0,
                    babyId = babyId,
                    dateTime = f.date.atTime(f.time),
                    title = f.title.trim(),
                    description = f.description.orNullIfBlank(),
                ),
            )
        }
    }
}

@Composable
fun MomentFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: MomentFormViewModel = viewModel { MomentFormViewModel(container, args.id, args.date) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Momento" else "Novo momento",
        status = status,
        onBack = onDone,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        TextInput(
            value = form.title,
            onValueChange = { v -> viewModel.update { it.copy(title = v, titleError = null) } },
            label = "O que aconteceu?",
            error = form.titleError,
        )
        if (form.title.isBlank()) {
            SuggestionChips(MOMENT_SUGGESTIONS, onPick = { v -> viewModel.update { it.copy(title = v, titleError = null) } })
        }
        DateField("Data", form.date, { d -> viewModel.update { it.copy(date = d) } })
        TimeField("Hora", form.time, { t -> viewModel.update { it.copy(time = t) } })
        TextInput(
            value = form.description,
            onValueChange = { v -> viewModel.update { it.copy(description = v) } },
            label = "Conte como foi (opcional)",
            singleLine = false,
            minLines = 3,
        )
        PhotoAttachmentSection(viewModel.photos)
    }
}
