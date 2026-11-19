package br.com.crescerbem.ui.screens.medications

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.MedicationSchedule
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.IntInput
import br.com.crescerbem.ui.components.LocalAppContainer
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
import java.time.LocalDateTime
import java.time.LocalTime

/** Preset da rota para abrir o formulário já como "dose administrada agora". */
const val PRESET_GIVEN = "GIVEN"


/** Pergunta exibida quando a dose faz parte de uma série: [count] doses vêm depois dela. */
data class ScopePrompt(val delete: Boolean, val count: Int)

data class MedicationForm(
    val name: String = "",
    val dosage: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.of(9, 0),
    val status: MedicationStatus = MedicationStatus.SCHEDULED,
    val givenDate: LocalDate = LocalDate.now(),
    val givenTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val notes: String = "",
    val repeat: Boolean = false,
    val intervalHours: String = "8",
    val repeatDays: String = "7",
    val nameError: String? = null,
    val repeatError: String? = null,
) {
    val scheduledAt: LocalDateTime get() = date.atTime(time)
    val givenAt: LocalDateTime get() = givenDate.atTime(givenTime)
}

class MedicationFormViewModel(
    container: AppContainer,
    id: Long?,
    presetDate: LocalDate?,
    preset: String?,
) : BaseFormViewModel(container, RecordType.MEDICATION, id) {

    private val _form = MutableStateFlow(
        defaultDateTime(presetDate).let { start ->
            val given = preset == PRESET_GIVEN
            MedicationForm(
                date = start.toLocalDate(),
                time = start.toLocalTime(),
                status = if (given) MedicationStatus.GIVEN else MedicationStatus.SCHEDULED,
                givenDate = start.toLocalDate(),
                givenTime = start.toLocalTime(),
            )
        },
    )
    val form: StateFlow<MedicationForm> = _form.asStateFlow()

    /** Dose como estava ao abrir (para saber o que mudou) e quantas doses da série vêm depois dela. */
    private var original: MedicationDose? = null
    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _scopePrompt = MutableStateFlow<ScopePrompt?>(null)
    val scopePrompt: StateFlow<ScopePrompt?> = _scopePrompt.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getMedication(recordId)?.let { m ->
                original = m
                if (m.status == MedicationStatus.SCHEDULED) {
                    _followingCount.value = container.recordRepository.followingInSeries(m).size
                }
                val given = m.givenAt ?: m.scheduledAt
                _form.value = MedicationForm(
                    name = m.name,
                    dosage = m.dosage.orEmpty(),
                    date = m.scheduledAt.toLocalDate(),
                    time = m.scheduledAt.toLocalTime(),
                    status = m.status,
                    givenDate = given.toLocalDate(),
                    givenTime = given.toLocalTime(),
                    notes = m.notes.orEmpty(),
                )
                m.babyId
            }
        }
    }

    fun update(transform: (MedicationForm) -> MedicationForm) = _form.update(transform)

    /** Marca como administrada agora (atalho do cartão "Já passou da hora"). */
    fun markGivenNow() = _form.update {
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        it.copy(status = MedicationStatus.GIVEN, givenDate = now.toLocalDate(), givenTime = now.toLocalTime())
    }

    fun dismissScopePrompt() {
        _scopePrompt.value = null
    }

    /** Excluir: se a dose tem outras da série pela frente, pergunta se vale só para ela. */
    override fun delete() {
        if (_followingCount.value > 0) {
            _scopePrompt.value = ScopePrompt(delete = true, count = _followingCount.value)
        } else {
            super.delete()
        }
    }

    fun deleteWithScope(includeFollowing: Boolean) {
        _scopePrompt.value = null
        val dose = original
        viewModelScope.launch {
            if (includeFollowing && dose != null) container.recordRepository.deleteFollowingInSeries(dose)
            deleteThisOnly()
        }
    }

    private fun deleteThisOnly() = super.delete()

    /** Salva a edição; [includeFollowing] diz se vale também para as próximas doses da série. */
    fun saveWithScope(includeFollowing: Boolean) {
        _scopePrompt.value = null
        save(includeFollowing)
    }

    private fun changedSchedule(f: MedicationForm): Boolean {
        val o = original ?: return false
        return f.name.trim() != o.name || f.dosage.orNullIfBlank() != o.dosage ||
            f.notes.orNullIfBlank() != o.notes || f.scheduledAt != o.scheduledAt
    }

    fun save(includeFollowing: Boolean? = null) {
        val f = _form.value
        if (f.name.isBlank()) {
            _form.update { it.copy(nameError = "Informe o nome do medicamento.") }
            return
        }
        val repeating = !isEdit && f.repeat && f.status == MedicationStatus.SCHEDULED
        val interval = f.intervalHours.toIntOrNull()
        val days = f.repeatDays.toIntOrNull()
        if (repeating) {
            val problem = MedicationSchedule.validate(interval, days)
            if (problem != null) {
                _form.update { it.copy(repeatError = problem) }
                return
            }
        }
        if (isEdit && includeFollowing == null && _followingCount.value > 0 && changedSchedule(f)) {
            _scopePrompt.value = ScopePrompt(delete = false, count = _followingCount.value)
            return
        }
        persistRecord { babyId ->
            val base = MedicationDose(
                id = recordId ?: 0,
                babyId = babyId,
                name = f.name.trim(),
                dosage = f.dosage.orNullIfBlank(),
                scheduledAt = f.scheduledAt,
                status = f.status,
                givenAt = if (f.status == MedicationStatus.GIVEN) f.givenAt else null,
                notes = f.notes.orNullIfBlank(),
                seriesId = original?.seriesId,
            )
            if (repeating && interval != null && days != null) {
                val times = MedicationSchedule.times(f.scheduledAt, interval, days)
                val series = if (times.size > 1) System.currentTimeMillis() else null
                val doses = times.map { base.copy(scheduledAt = it, seriesId = series) }
                container.recordRepository.saveMedicationDoses(doses).first()
            } else {
                val dose = original
                // Aplica às próximas antes de salvar: a busca usa o horário original desta dose.
                if (includeFollowing == true && dose != null) {
                    container.recordRepository.applyEditToFollowing(dose, base)
                }
                container.recordRepository.saveMedication(base)
            }
        }
    }
}

@Composable
fun MedicationFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: MedicationFormViewModel = viewModel {
        MedicationFormViewModel(container, args.id, args.date, args.preset)
    }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
    val scopePrompt by viewModel.scopePrompt.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    scopePrompt?.let { prompt ->
        val doses = if (prompt.count == 1) "1 dose" else "${prompt.count} doses"
        AlertDialog(
            onDismissRequest = viewModel::dismissScopePrompt,
            title = { Text(if (prompt.delete) "Excluir quais doses?" else "Aplicar a quais doses?") },
            text = {
                Text(
                    "Essa dose faz parte de uma série. Há mais $doses agendada(s) depois dela. " +
                        if (prompt.delete) {
                            "Você quer excluir só esta ou também as próximas?"
                        } else {
                            "Você quer mudar só esta ou também as próximas? " +
                                "Se o horário mudou, as próximas mudam o mesmo tanto. Doses já realizadas não são alteradas."
                        },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (prompt.delete) viewModel.deleteWithScope(true) else viewModel.saveWithScope(true)
                    },
                ) { Text("Esta e as próximas") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (prompt.delete) viewModel.deleteWithScope(false) else viewModel.saveWithScope(false)
                    },
                ) { Text("Só esta dose") }
            },
        )
    }

    FormScaffold(
        title = if (viewModel.isEdit) "Medicamento" else "Novo medicamento",
        status = status,
        onBack = onDone,
        onSave = { viewModel.save() },
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        InfoCard(Texts.MEDICATION_NOTE)
        if (followingCount > 0) {
            InfoCard(
                "Esta dose faz parte de uma série: há mais $followingCount agendada(s) depois dela. " +
                    "Ao salvar mudanças, você escolhe se valem só para esta ou para as próximas.",
            )
        }
        val pastAndOpen = viewModel.isEdit &&
            form.status == MedicationStatus.SCHEDULED &&
            form.scheduledAt.isBefore(LocalDateTime.now())
        if (pastAndOpen) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Não registrado: o horário dessa dose já passou. Ela foi realizada?",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::markGivenNow) { Text("Realizado") }
                        OutlinedButton(onClick = { viewModel.update { it.copy(status = MedicationStatus.SKIPPED) } }) {
                            Text("Não realizado")
                        }
                    }
                }
            }
        }
        TextInput(
            value = form.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v, nameError = null) } },
            label = "Nome do medicamento",
            error = form.nameError,
        )
        TextInput(
            value = form.dosage,
            onValueChange = { v -> viewModel.update { it.copy(dosage = v) } },
            label = "Dose (opcional)",
            supporting = "Como na receita. Ex.: 5 ml, 10 gotas",
        )
        ChoiceChips(
            title = "Situação",
            options = MedicationStatus.entries,
            selected = form.status,
            optionLabel = { it.label },
            onSelect = { s -> viewModel.update { it.copy(status = s) } },
        )
        DateField(
            if (form.status == MedicationStatus.SCHEDULED) "Data" else "Data prevista",
            form.date,
            { d -> viewModel.update { it.copy(date = d) } },
        )
        TimeField(
            if (form.status == MedicationStatus.SCHEDULED) "Horário" else "Horário previsto",
            form.time,
            { t -> viewModel.update { it.copy(time = t) } },
        )
        if (form.status == MedicationStatus.GIVEN) {
            DateField("Data em que foi dada", form.givenDate, { d -> viewModel.update { it.copy(givenDate = d) } })
            TimeField("Horário em que foi dada", form.givenTime, { t -> viewModel.update { it.copy(givenTime = t) } })
        }
        if (!viewModel.isEdit && form.status == MedicationStatus.SCHEDULED) {
            SwitchRow(
                label = "Repetir esse horário",
                checked = form.repeat,
                onCheckedChange = { v -> viewModel.update { it.copy(repeat = v, repeatError = null) } },
                supporting = "Agenda várias doses de uma vez, de tantas em tantas horas, até o limite de dias.",
            )
            if (form.repeat) {
                IntInput(
                    value = form.intervalHours,
                    onValueChange = { v -> viewModel.update { it.copy(intervalHours = v, repeatError = null) } },
                    label = "A cada quantas horas",
                    suffix = "h",
                )
                val interval = form.intervalHours.toIntOrNull()
                val days = form.repeatDays.toIntOrNull()
                val valid = MedicationSchedule.validate(interval, days) == null
                IntInput(
                    value = form.repeatDays,
                    onValueChange = { v -> viewModel.update { it.copy(repeatDays = v, repeatError = null) } },
                    label = "Repetir por quantos dias",
                    suffix = "dias",
                    error = form.repeatError,
                    supporting = if (valid && interval != null && days != null) {
                        val times = MedicationSchedule.times(form.scheduledAt, interval, days)
                        "Serão agendadas ${times.size} doses, a última em ${Formats.dateTime(times.last())}."
                    } else {
                        "Limite de ${MedicationSchedule.MAX_DAYS} dias."
                    },
                )
            }
        }
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
    }
}
