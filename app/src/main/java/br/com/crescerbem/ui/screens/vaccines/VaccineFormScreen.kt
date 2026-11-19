package br.com.crescerbem.ui.screens.vaccines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.VaccineDisplayStatus
import br.com.crescerbem.domain.VaccineSchedule
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.PhotoAttachmentSection
import br.com.crescerbem.ui.components.SuggestionChips
import br.com.crescerbem.ui.components.TextInput
import br.com.crescerbem.ui.components.orNullIfBlank
import br.com.crescerbem.ui.navigation.FormArgs
import br.com.crescerbem.ui.theme.StatusColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Ícone de apoio ao status, para não depender só da cor. */
fun statusIcon(status: VaccineDisplayStatus): ImageVector = when (status) {
    VaccineDisplayStatus.APPLIED -> Icons.Filled.Check
    VaccineDisplayStatus.PLANNED -> Icons.Filled.CalendarMonth
    VaccineDisplayStatus.PENDING -> Icons.Filled.Schedule
    VaccineDisplayStatus.OVERDUE -> Icons.Filled.Warning
    VaccineDisplayStatus.NOT_APPLICABLE -> Icons.Filled.Block
}

fun statusColor(status: VaccineDisplayStatus): Color = when (status) {
    VaccineDisplayStatus.APPLIED -> StatusColors.done
    VaccineDisplayStatus.PENDING -> StatusColors.pending
    VaccineDisplayStatus.OVERDUE -> StatusColors.overdue
    VaccineDisplayStatus.PLANNED, VaccineDisplayStatus.NOT_APPLICABLE -> StatusColors.neutral
}

val VaccineStatus.formLabel: String
    get() = when (this) {
        VaccineStatus.SCHEDULED -> "Prevista"
        VaccineStatus.APPLIED -> "Aplicada"
        VaccineStatus.NOT_APPLICABLE -> "Não se aplica"
    }

private val NAME_SUGGESTIONS: List<String> =
    VaccineSchedule.suggestions.map { it.name }.distinct() + listOf("Influenza (gripe)", "COVID-19")

data class VaccineForm(
    val name: String = "",
    val dose: String = "",
    val status: VaccineStatus = VaccineStatus.APPLIED,
    val scheduledDate: LocalDate? = null,
    val appliedDate: LocalDate? = LocalDate.now(),
    val recommendedAgeMonths: Int? = null,
    val lot: String = "",
    val place: String = "",
    val reaction: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val dateError: String? = null,
)

class VaccineFormViewModel(container: AppContainer, id: Long?, presetDate: LocalDate?) :
    BaseFormViewModel(container, RecordType.VACCINE, id) {

    private val _form = MutableStateFlow(initialForm(presetDate))
    val form: StateFlow<VaccineForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getVaccine(recordId)?.let { v ->
                _form.value = VaccineForm(
                    name = v.name,
                    dose = v.dose.orEmpty(),
                    status = v.status,
                    scheduledDate = v.scheduledDate,
                    appliedDate = v.appliedDate ?: if (v.status == VaccineStatus.SCHEDULED) LocalDate.now() else null,
                    recommendedAgeMonths = v.recommendedAgeMonths,
                    lot = v.lot.orEmpty(),
                    place = v.place.orEmpty(),
                    reaction = v.reaction.orEmpty(),
                    notes = v.notes.orEmpty(),
                )
                v.babyId
            }
        }
    }

    fun update(transform: (VaccineForm) -> VaccineForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        val nameError = if (f.name.isBlank()) "Informe o nome da vacina." else null
        val dateError = when {
            f.status == VaccineStatus.APPLIED && f.appliedDate == null -> "Informe a data da aplicação."
            f.status == VaccineStatus.SCHEDULED && f.scheduledDate == null -> "Informe a data prevista."
            else -> null
        }
        _form.update { it.copy(nameError = nameError, dateError = dateError) }
        if (nameError != null || dateError != null) return
        persistRecord { babyId ->
            container.recordRepository.saveVaccine(
                Vaccine(
                    id = recordId ?: 0,
                    babyId = babyId,
                    name = f.name.trim(),
                    dose = f.dose.orNullIfBlank(),
                    recommendedAgeMonths = f.recommendedAgeMonths,
                    scheduledDate = f.scheduledDate,
                    appliedDate = if (f.status == VaccineStatus.APPLIED) f.appliedDate else null,
                    status = f.status,
                    lot = f.lot.orNullIfBlank(),
                    place = f.place.orNullIfBlank(),
                    reaction = if (f.status == VaccineStatus.APPLIED) f.reaction.orNullIfBlank() else null,
                    notes = f.notes.orNullIfBlank(),
                ),
            )
        }
    }

    private companion object {
        /** Registro novo: data passada ou de hoje = vacina aplicada; data futura = prevista. */
        fun initialForm(date: LocalDate?): VaccineForm {
            val today = LocalDate.now()
            return if (date != null && date.isAfter(today)) {
                VaccineForm(status = VaccineStatus.SCHEDULED, scheduledDate = date, appliedDate = null)
            } else {
                VaccineForm(status = VaccineStatus.APPLIED, appliedDate = date ?: today)
            }
        }
    }
}

@Composable
fun VaccineFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: VaccineFormViewModel = viewModel { VaccineFormViewModel(container, args.id, args.date) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Vacina" else "Nova vacina",
        status = status,
        onBack = onDone,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        InfoCard(Texts.VACCINE_NOTE)
        TextInput(
            value = form.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v, nameError = null) } },
            label = "Nome da vacina",
            error = form.nameError,
        )
        if (form.name.isBlank()) {
            SuggestionChips(NAME_SUGGESTIONS, onPick = { v -> viewModel.update { it.copy(name = v, nameError = null) } })
        }
        TextInput(
            value = form.dose,
            onValueChange = { v -> viewModel.update { it.copy(dose = v) } },
            label = "Dose (ex.: 1ª dose, reforço)",
        )
        ChoiceChips(
            title = "Situação",
            options = VaccineStatus.entries,
            selected = form.status,
            optionLabel = { it.formLabel },
            onSelect = { s ->
                viewModel.update {
                    it.copy(
                        status = s,
                        appliedDate = if (s == VaccineStatus.APPLIED) it.appliedDate ?: LocalDate.now() else it.appliedDate,
                        dateError = null,
                    )
                }
            },
        )
        when (form.status) {
            VaccineStatus.APPLIED -> {
                DateField(
                    label = "Data da aplicação",
                    value = form.appliedDate,
                    onValueChange = { d -> viewModel.update { it.copy(appliedDate = d, dateError = null) } },
                    error = form.dateError,
                )
                TextInput(
                    value = form.lot,
                    onValueChange = { v -> viewModel.update { it.copy(lot = v) } },
                    label = "Lote (opcional)",
                )
                TextInput(
                    value = form.place,
                    onValueChange = { v -> viewModel.update { it.copy(place = v) } },
                    label = "Local (opcional)",
                )
                TextInput(
                    value = form.reaction,
                    onValueChange = { v -> viewModel.update { it.copy(reaction = v) } },
                    label = "Reação observada (opcional)",
                    singleLine = false,
                    minLines = 2,
                )
            }
            VaccineStatus.SCHEDULED -> DateField(
                label = "Data prevista",
                value = form.scheduledDate,
                onValueChange = { d -> viewModel.update { it.copy(scheduledDate = d, dateError = null) } },
                error = form.dateError,
            )
            VaccineStatus.NOT_APPLICABLE -> Unit
        }
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
        PhotoAttachmentSection(viewModel.photos, title = "Foto da caderneta", hint = "Opcional. A foto fica só neste aparelho.")
    }
}
