package br.com.crescerbem.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
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
import java.time.LocalDateTime
import java.time.LocalTime

private val TITLE_SUGGESTIONS = listOf(
    "Pediatra",
    "Puericultura",
    "Teste do pezinho",
    "Teste da orelhinha",
    "Teste do olhinho",
    "Retorno",
)

data class AppointmentForm(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.of(9, 0),
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val professional: String = "",
    val place: String = "",
    val reason: String = "",
    val guidance: String = "",
    val notes: String = "",
    val titleError: String? = null,
) {
    val dateTime: LocalDateTime get() = date.atTime(time)
}

class AppointmentFormViewModel(container: AppContainer, id: Long?, presetDate: LocalDate?) :
    BaseFormViewModel(container, RecordType.APPOINTMENT, id) {

    private val _form = MutableStateFlow(
        defaultDateTime(presetDate).let { AppointmentForm(date = it.toLocalDate(), time = it.toLocalTime()) },
    )
    val form: StateFlow<AppointmentForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getAppointment(recordId)?.let { a ->
                _form.value = AppointmentForm(
                    title = a.title,
                    date = a.dateTime.toLocalDate(),
                    time = a.dateTime.toLocalTime(),
                    status = a.status,
                    professional = a.professional.orEmpty(),
                    place = a.place.orEmpty(),
                    reason = a.reason.orEmpty(),
                    guidance = a.guidance.orEmpty(),
                    notes = a.notes.orEmpty(),
                )
                a.babyId
            }
        }
    }

    fun update(transform: (AppointmentForm) -> AppointmentForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        if (f.title.isBlank()) {
            _form.update { it.copy(titleError = "Informe o tipo de consulta ou especialidade.") }
            return
        }
        persistRecord { babyId ->
            container.recordRepository.saveAppointment(
                Appointment(
                    id = recordId ?: 0,
                    babyId = babyId,
                    title = f.title.trim(),
                    dateTime = f.dateTime,
                    status = f.status,
                    professional = f.professional.orNullIfBlank(),
                    place = f.place.orNullIfBlank(),
                    reason = f.reason.orNullIfBlank(),
                    guidance = f.guidance.orNullIfBlank(),
                    notes = f.notes.orNullIfBlank(),
                ),
            )
        }
    }
}

@Composable
fun AppointmentFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AppointmentFormViewModel = viewModel { AppointmentFormViewModel(container, args.id, args.date) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Consulta" else "Nova consulta",
        status = status,
        onBack = onDone,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        val pastAndOpen = viewModel.isEdit &&
            form.status == AppointmentStatus.SCHEDULED &&
            form.dateTime.isBefore(LocalDateTime.now())
        if (pastAndOpen) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Essa consulta já passou. Ela foi realizada?", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.update { it.copy(status = AppointmentStatus.DONE) } }) {
                            Text("Sim, foi realizada")
                        }
                        OutlinedButton(onClick = { viewModel.update { it.copy(status = AppointmentStatus.CANCELED) } }) {
                            Text("Foi cancelada")
                        }
                    }
                }
            }
        }
        TextInput(
            value = form.title,
            onValueChange = { v -> viewModel.update { it.copy(title = v, titleError = null) } },
            label = "Consulta ou especialidade",
            error = form.titleError,
        )
        if (form.title.isBlank()) {
            SuggestionChips(TITLE_SUGGESTIONS, onPick = { v -> viewModel.update { it.copy(title = v, titleError = null) } })
        }
        DateField("Data", form.date, { d -> viewModel.update { it.copy(date = d) } })
        TimeField("Horário", form.time, { t -> viewModel.update { it.copy(time = t) } })
        ChoiceChips(
            title = "Situação",
            options = AppointmentStatus.entries,
            selected = form.status,
            optionLabel = { it.label },
            onSelect = { s -> viewModel.update { it.copy(status = s) } },
        )
        TextInput(
            value = form.professional,
            onValueChange = { v -> viewModel.update { it.copy(professional = v) } },
            label = "Profissional (opcional)",
        )
        TextInput(
            value = form.place,
            onValueChange = { v -> viewModel.update { it.copy(place = v) } },
            label = "Local (opcional)",
        )
        TextInput(
            value = form.reason,
            onValueChange = { v -> viewModel.update { it.copy(reason = v) } },
            label = "Motivo (opcional)",
            singleLine = false,
            minLines = 2,
        )
        if (form.status == AppointmentStatus.DONE) {
            TextInput(
                value = form.guidance,
                onValueChange = { v -> viewModel.update { it.copy(guidance = v) } },
                label = "Orientações recebidas",
                singleLine = false,
                minLines = 3,
            )
        }
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
        PhotoAttachmentSection(
            viewModel.photos,
            title = "Fotos",
            hint = "Receitas, pedidos de exame ou resultados. Ficam só neste aparelho.",
        )
    }
}
