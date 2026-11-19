package br.com.crescerbem.ui.screens.growth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Plausibility
import br.com.crescerbem.domain.decimals
import br.com.crescerbem.domain.label
import br.com.crescerbem.domain.unit
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.DecimalInput
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.PhotoAttachmentSection
import br.com.crescerbem.ui.components.PlausibilityDialog
import br.com.crescerbem.ui.components.SuggestionChips
import br.com.crescerbem.ui.components.TextInput
import br.com.crescerbem.ui.components.orNullIfBlank
import br.com.crescerbem.ui.navigation.FormArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

private val PLACE_SUGGESTIONS = listOf("Casa", "Posto de saúde", "Consultório", "Farmácia")

data class GrowthForm(
    val type: MeasurementType = MeasurementType.WEIGHT,
    val date: LocalDate = LocalDate.now(),
    val value: String = "",
    val place: String = "",
    val notes: String = "",
    val valueError: String? = null,
    val dateError: String? = null,
    val warning: String? = null,
)

class GrowthFormViewModel(
    container: AppContainer,
    id: Long?,
    presetDate: LocalDate?,
    presetType: MeasurementType?,
) : BaseFormViewModel(container, RecordType.GROWTH, id) {

    private val _form = MutableStateFlow(
        GrowthForm(type = presetType ?: MeasurementType.WEIGHT, date = presetDate ?: LocalDate.now()),
    )
    val form: StateFlow<GrowthForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getGrowth(recordId)?.let { m ->
                _form.value = GrowthForm(
                    type = m.type,
                    date = m.date,
                    value = Formats.decimal(m.value, m.type.decimals),
                    place = m.place.orEmpty(),
                    notes = m.notes.orEmpty(),
                )
                m.babyId
            }
        }
    }

    fun update(transform: (GrowthForm) -> GrowthForm) = _form.update(transform)

    fun dismissWarning() = _form.update { it.copy(warning = null) }

    fun save(confirmed: Boolean = false) {
        val f = _form.value
        val value = Formats.parseDecimal(f.value)
        val valueError = when {
            value == null -> "Informe o valor em ${f.type.unit}."
            value <= 0 -> "O valor precisa ser maior que zero."
            else -> null
        }
        val dateError = if (f.date.isAfter(LocalDate.now())) "A data da medida não pode estar no futuro." else null
        _form.update { it.copy(valueError = valueError, dateError = dateError, warning = null) }
        if (valueError != null || dateError != null || value == null) return
        if (!confirmed) {
            Plausibility.measurementWarning(f.type, value)?.let { warning ->
                _form.update { it.copy(warning = warning) }
                return
            }
        }
        persistRecord { babyId ->
            container.recordRepository.saveGrowth(
                GrowthMeasurement(
                    id = recordId ?: 0,
                    babyId = babyId,
                    date = f.date,
                    type = f.type,
                    value = value,
                    place = f.place.orNullIfBlank(),
                    notes = f.notes.orNullIfBlank(),
                ),
            )
        }
    }
}

@Composable
fun GrowthFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val presetType = args.preset?.let { name -> MeasurementType.entries.firstOrNull { it.name == name } }
    val viewModel: GrowthFormViewModel = viewModel { GrowthFormViewModel(container, args.id, args.date, presetType) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Medida" else "Nova medida",
        status = status,
        onBack = onDone,
        onSave = { viewModel.save() },
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        ChoiceChips(
            title = "O que você mediu?",
            options = MeasurementType.entries,
            selected = form.type,
            optionLabel = { it.label },
            onSelect = { t -> viewModel.update { it.copy(type = t, valueError = null) } },
        )
        DateField(
            label = "Data",
            value = form.date,
            onValueChange = { d -> viewModel.update { it.copy(date = d, dateError = null) } },
            error = form.dateError,
        )
        DecimalInput(
            value = form.value,
            onValueChange = { v -> viewModel.update { it.copy(value = v, valueError = null) } },
            label = form.type.label,
            suffix = form.type.unit,
            error = form.valueError,
            supporting = if (form.type == MeasurementType.WEIGHT) "Em quilos, ex.: 4,250" else "Em centímetros, ex.: 54,5",
        )
        TextInput(
            value = form.place,
            onValueChange = { v -> viewModel.update { it.copy(place = v) } },
            label = "Onde foi medido (opcional)",
        )
        SuggestionChips(PLACE_SUGGESTIONS, onPick = { v -> viewModel.update { it.copy(place = v) } })
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
        PhotoAttachmentSection(viewModel.photos, hint = "Ex.: foto da balança ou da caderneta.")
    }

    form.warning?.let { warning ->
        PlausibilityDialog(
            message = warning,
            onConfirm = { viewModel.save(confirmed = true) },
            onDismiss = viewModel::dismissWarning,
        )
    }
}
