package br.com.crescerbem.ui.screens.health

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.Intensity
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.domain.AttentionSigns
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Plausibility
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.AttentionCard
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.DecimalInput
import br.com.crescerbem.ui.components.DisclaimerCard
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.MultiChoiceChips
import br.com.crescerbem.ui.components.PhotoAttachmentSection
import br.com.crescerbem.ui.components.PlausibilityDialog
import br.com.crescerbem.ui.components.SwitchRow
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

data class HealthEventForm(
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val symptoms: Set<Symptom> = emptySet(),
    val otherSymptom: String = "",
    val intensity: Intensity = Intensity.MILD,
    val temperature: String = "",
    val duration: String = "",
    val actionsTaken: String = "",
    val soughtCare: Boolean = false,
    val notes: String = "",
    val symptomError: String? = null,
    val temperatureError: String? = null,
    val warning: String? = null,
    /** Sinais de atenção calculados ao salvar; mostrados antes de fechar o formulário. */
    val attention: List<String> = emptyList(),
)

class HealthEventFormViewModel(container: AppContainer, id: Long?, presetDate: LocalDate?) :
    BaseFormViewModel(container, RecordType.HEALTH_EVENT, id) {

    private val _form = MutableStateFlow(
        defaultDateTime(presetDate).let { HealthEventForm(date = it.toLocalDate(), time = it.toLocalTime()) },
    )
    val form: StateFlow<HealthEventForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getHealthEvent(recordId)?.let { e ->
                _form.value = HealthEventForm(
                    date = e.dateTime.toLocalDate(),
                    time = e.dateTime.toLocalTime(),
                    symptoms = e.symptoms.toSet(),
                    otherSymptom = e.otherSymptom.orEmpty(),
                    intensity = e.intensity,
                    temperature = e.temperatureCelsius?.let { Formats.decimal(it, 1) }.orEmpty(),
                    duration = e.duration.orEmpty(),
                    actionsTaken = e.actionsTaken.orEmpty(),
                    soughtCare = e.soughtCare,
                    notes = e.notes.orEmpty(),
                )
                e.babyId
            }
        }
    }

    fun update(transform: (HealthEventForm) -> HealthEventForm) = _form.update(transform)

    fun toggleSymptom(symptom: Symptom) = _form.update {
        val next = if (symptom in it.symptoms) it.symptoms - symptom else it.symptoms + symptom
        it.copy(symptoms = next, symptomError = null)
    }

    fun dismissWarning() = _form.update { it.copy(warning = null) }

    fun save(confirmed: Boolean = false) {
        val f = _form.value
        val symptomError = if (f.symptoms.isEmpty() && f.otherSymptom.isBlank()) "Escolha pelo menos um sintoma." else null
        val temperature = Formats.parseDecimal(f.temperature)
        val temperatureError = if (f.temperature.isNotBlank() && temperature == null) "Digite só números, ex.: 37,8" else null
        _form.update { it.copy(symptomError = symptomError, temperatureError = temperatureError, warning = null) }
        if (symptomError != null || temperatureError != null) return
        if (!confirmed && temperature != null) {
            Plausibility.temperatureWarning(temperature)?.let { warning ->
                _form.update { it.copy(warning = warning) }
                return
            }
        }
        val symptoms = (if (f.otherSymptom.isNotBlank()) f.symptoms + Symptom.OTHER else f.symptoms)
            .sortedBy { it.ordinal }
        persistRecord { babyId ->
            val event = HealthEvent(
                id = recordId ?: 0,
                babyId = babyId,
                dateTime = f.date.atTime(f.time),
                symptoms = symptoms,
                otherSymptom = f.otherSymptom.orNullIfBlank(),
                intensity = f.intensity,
                temperatureCelsius = temperature,
                duration = f.duration.orNullIfBlank(),
                actionsTaken = f.actionsTaken.orNullIfBlank(),
                soughtCare = f.soughtCare,
                notes = f.notes.orNullIfBlank(),
            )
            val id = container.recordRepository.saveHealthEvent(event)
            container.babyRepository.get(babyId)?.let { baby ->
                val signs = AttentionSigns.evaluate(event, baby.birthDate)
                _form.update { it.copy(attention = signs) }
            }
            id
        }
    }
}

@Composable
fun HealthEventFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HealthEventFormViewModel = viewModel { HealthEventFormViewModel(container, args.id, args.date) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done, form.attention) {
        if (status.done && form.attention.isEmpty()) onDone()
    }

    FormScaffold(
        title = if (viewModel.isEdit) "Sintoma" else "Registrar sintoma",
        status = status,
        onBack = onDone,
        onSave = { viewModel.save() },
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        DateField("Data", form.date, { d -> viewModel.update { it.copy(date = d) } })
        TimeField("Hora", form.time, { t -> viewModel.update { it.copy(time = t) } })
        MultiChoiceChips(
            title = "O que você observou?",
            options = Symptom.entries.filter { it != Symptom.OTHER },
            selected = form.symptoms,
            optionLabel = { it.label },
            onToggle = viewModel::toggleSymptom,
            error = form.symptomError,
        )
        TextInput(
            value = form.otherSymptom,
            onValueChange = { v -> viewModel.update { it.copy(otherSymptom = v, symptomError = null) } },
            label = "Outro sintoma (opcional)",
        )
        ChoiceChips(
            title = "Intensidade",
            options = Intensity.entries,
            selected = form.intensity,
            optionLabel = { it.label },
            onSelect = { i -> viewModel.update { it.copy(intensity = i) } },
        )
        DecimalInput(
            value = form.temperature,
            onValueChange = { v -> viewModel.update { it.copy(temperature = v, temperatureError = null) } },
            label = "Temperatura (opcional)",
            suffix = "°C",
            error = form.temperatureError,
            supporting = "Medida na axila, ex.: 37,8",
        )
        TextInput(
            value = form.duration,
            onValueChange = { v -> viewModel.update { it.copy(duration = v) } },
            label = "Há quanto tempo? (opcional)",
            supporting = "Ex.: desde a manhã, 2 dias",
        )
        TextInput(
            value = form.actionsTaken,
            onValueChange = { v -> viewModel.update { it.copy(actionsTaken = v) } },
            label = "O que foi feito (opcional)",
            singleLine = false,
            minLines = 2,
        )
        SwitchRow(
            label = "Procurou atendimento",
            checked = form.soughtCare,
            onCheckedChange = { v -> viewModel.update { it.copy(soughtCare = v) } },
        )
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
        PhotoAttachmentSection(viewModel.photos, hint = "Ex.: manchas na pele. A foto fica só neste aparelho.")
        DisclaimerCard()
    }

    form.warning?.let { warning ->
        PlausibilityDialog(
            message = warning,
            onConfirm = { viewModel.save(confirmed = true) },
            onDismiss = viewModel::dismissWarning,
        )
    }

    if (status.done && form.attention.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Registro salvo") },
            text = { AttentionCard(form.attention) },
            confirmButton = { TextButton(onClick = onDone) { Text("Entendi") } },
        )
    }
}
