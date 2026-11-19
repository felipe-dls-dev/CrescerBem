package br.com.crescerbem.ui.screens.feeding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Acceptance
import br.com.crescerbem.data.model.BreastSide
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.FoodTexture
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.isSolid
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.IntInput
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.PhotoAttachmentSection
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

data class FeedingForm(
    val type: FeedingType = FeedingType.BREAST_MILK,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val side: BreastSide? = null,
    val duration: String = "",
    val amount: String = "",
    val food: String = "",
    val texture: FoodTexture? = null,
    val acceptance: Acceptance? = null,
    val firstTime: Boolean = false,
    val reaction: String = "",
    val notes: String = "",
    val foodError: String? = null,
)

private val FeedingType.usesFood: Boolean get() = isSolid || this == FeedingType.OTHER

class FeedingFormViewModel(
    container: AppContainer,
    id: Long?,
    presetDate: LocalDate?,
    presetType: FeedingType?,
) : BaseFormViewModel(container, RecordType.FEEDING, id) {

    private val _form = MutableStateFlow(
        defaultDateTime(presetDate).let {
            FeedingForm(type = presetType ?: FeedingType.BREAST_MILK, date = it.toLocalDate(), time = it.toLocalTime())
        },
    )
    val form: StateFlow<FeedingForm> = _form.asStateFlow()

    init {
        start { recordId ->
            container.recordRepository.getFeeding(recordId)?.let { f ->
                _form.value = FeedingForm(
                    type = f.type,
                    date = f.dateTime.toLocalDate(),
                    time = f.dateTime.toLocalTime(),
                    side = f.breastSide,
                    duration = f.durationMinutes?.toString().orEmpty(),
                    amount = f.amountMl?.toString().orEmpty(),
                    food = f.food.orEmpty(),
                    texture = f.texture,
                    acceptance = f.acceptance,
                    firstTime = f.firstTime,
                    reaction = f.reaction.orEmpty(),
                    notes = f.notes.orEmpty(),
                )
                f.babyId
            }
        }
    }

    fun update(transform: (FeedingForm) -> FeedingForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        if (f.type.usesFood && f.food.isBlank()) {
            _form.update { it.copy(foodError = "Conte o que foi oferecido.") }
            return
        }
        // Só grava os campos que fazem sentido para o tipo escolhido.
        val isBreast = f.type == FeedingType.BREAST_MILK
        persistRecord { babyId ->
            container.recordRepository.saveFeeding(
                Feeding(
                    id = recordId ?: 0,
                    babyId = babyId,
                    dateTime = f.date.atTime(f.time),
                    type = f.type,
                    breastSide = if (isBreast) f.side else null,
                    durationMinutes = if (isBreast) Formats.parseInt(f.duration) else null,
                    amountMl = if (f.type.isSolid) null else Formats.parseInt(f.amount),
                    food = if (f.type.usesFood) f.food.orNullIfBlank() else null,
                    texture = if (f.type.isSolid) f.texture else null,
                    acceptance = if (f.type.usesFood) f.acceptance else null,
                    firstTime = f.type.usesFood && f.firstTime,
                    reaction = if (f.type.usesFood) f.reaction.orNullIfBlank() else null,
                    notes = f.notes.orNullIfBlank(),
                ),
            )
        }
    }
}

@Composable
fun FeedingFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val presetType = args.preset?.let { name -> FeedingType.entries.firstOrNull { it.name == name } }
    val viewModel: FeedingFormViewModel = viewModel { FeedingFormViewModel(container, args.id, args.date, presetType) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Alimentação" else "Registrar alimentação",
        status = status,
        onBack = onDone,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEdit) viewModel::delete else null,
    ) {
        ChoiceChips(
            title = "Tipo",
            options = FeedingType.entries,
            selected = form.type,
            optionLabel = { it.label },
            onSelect = { t -> viewModel.update { it.copy(type = t, foodError = null) } },
        )
        DateField("Data", form.date, { d -> viewModel.update { it.copy(date = d) } })
        TimeField("Hora", form.time, { t -> viewModel.update { it.copy(time = t) } })

        when (form.type) {
            FeedingType.BREAST_MILK -> {
                ChoiceChips(
                    title = "Lado",
                    options = BreastSide.entries,
                    selected = form.side,
                    optionLabel = { it.label },
                    onSelect = { s -> viewModel.update { it.copy(side = s) } },
                )
                IntInput(
                    value = form.duration,
                    onValueChange = { v -> viewModel.update { it.copy(duration = v) } },
                    label = "Duração (opcional)",
                    suffix = "min",
                )
                IntInput(
                    value = form.amount,
                    onValueChange = { v -> viewModel.update { it.copy(amount = v) } },
                    label = "Quantidade (opcional)",
                    suffix = "ml",
                    supporting = "Se foi leite materno na mamadeira ou no copinho.",
                )
            }
            FeedingType.FORMULA, FeedingType.WATER -> IntInput(
                value = form.amount,
                onValueChange = { v -> viewModel.update { it.copy(amount = v) } },
                label = "Quantidade",
                suffix = "ml",
            )
            FeedingType.FRUIT, FeedingType.SOLID_MEAL, FeedingType.OTHER -> {
                TextInput(
                    value = form.food,
                    onValueChange = { v -> viewModel.update { it.copy(food = v, foodError = null) } },
                    label = if (form.type == FeedingType.FRUIT) "Qual fruta?" else "O que foi oferecido?",
                    error = form.foodError,
                )
                if (form.type.isSolid) {
                    ChoiceChips(
                        title = "Forma",
                        options = FoodTexture.entries,
                        selected = form.texture,
                        optionLabel = { it.label },
                        onSelect = { t -> viewModel.update { it.copy(texture = t) } },
                    )
                } else {
                    IntInput(
                        value = form.amount,
                        onValueChange = { v -> viewModel.update { it.copy(amount = v) } },
                        label = "Quantidade (opcional)",
                        suffix = "ml",
                    )
                }
                ChoiceChips(
                    title = "Aceitação",
                    options = Acceptance.entries,
                    selected = form.acceptance,
                    optionLabel = { it.label },
                    onSelect = { a -> viewModel.update { it.copy(acceptance = a) } },
                )
                SwitchRow(
                    label = "Primeira vez com este alimento",
                    supporting = "Ajuda a acompanhar a introdução alimentar.",
                    checked = form.firstTime,
                    onCheckedChange = { v -> viewModel.update { it.copy(firstTime = v) } },
                )
                TextInput(
                    value = form.reaction,
                    onValueChange = { v -> viewModel.update { it.copy(reaction = v) } },
                    label = "Reação observada (opcional)",
                    supporting = "Ex.: manchas na pele, vômito, mudança nas fezes.",
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
        PhotoAttachmentSection(viewModel.photos)
    }
}
