package br.com.crescerbem.ui.screens.baby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Sex
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.BaseFormViewModel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.DecimalInput
import br.com.crescerbem.ui.components.DropdownField
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.FormScaffold
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.PhotoAttachmentSection
import br.com.crescerbem.ui.components.SavedRef
import br.com.crescerbem.ui.components.SwitchRow
import br.com.crescerbem.ui.components.TextInput
import br.com.crescerbem.ui.components.TimeField
import br.com.crescerbem.ui.components.orNullIfBlank
import br.com.crescerbem.ui.navigation.FormArgs
import br.com.crescerbem.ui.theme.BabyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

val BLOOD_TYPES = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

/** Opções de sexo com a escolha explícita de não informar (coleta mínima). */
private enum class SexOption(val label: String, val sex: Sex?) {
    FEMALE("Feminino", Sex.FEMALE),
    MALE("Masculino", Sex.MALE),
    NONE("Prefiro não informar", null),
}

data class BabyForm(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val birthTime: LocalTime? = null,
    val sex: Sex? = null,
    val bloodType: String? = null,
    val notes: String = "",
    val birthWeight: String = "",
    val birthLength: String = "",
    val birthHead: String = "",
    val addSuggestedVaccines: Boolean = true,
    val colorIndex: Int = 0,
    val nameError: String? = null,
    val dateError: String? = null,
    val measureError: String? = null,
)

class BabyFormViewModel(container: AppContainer, id: Long?) :
    BaseFormViewModel(container, RecordType.BABY, id, requiresBaby = false) {

    private val _form = MutableStateFlow(BabyForm())
    val form: StateFlow<BabyForm> = _form.asStateFlow()

    init {
        if (id == null) {
            // Bebê novo já começa com a próxima cor livre da paleta (a pessoa pode trocar).
            viewModelScope.launch {
                val next = container.babyRepository.nextColorIndex(BabyPalette.size)
                _form.update { it.copy(colorIndex = next) }
            }
        }
        start { recordId ->
            container.babyRepository.get(recordId)?.let { baby ->
                _form.value = BabyForm(
                    name = baby.name,
                    birthDate = baby.birthDate,
                    birthTime = baby.birthTime,
                    sex = baby.sex,
                    bloodType = baby.bloodType,
                    notes = baby.notes.orEmpty(),
                    colorIndex = baby.colorIndex,
                )
                baby.id
            }
        }
    }

    fun update(transform: (BabyForm) -> BabyForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        val nameError = if (f.name.isBlank()) "Informe o nome ou um apelido." else null
        val dateError = when {
            f.birthDate == null -> "Informe a data de nascimento."
            f.birthDate.isAfter(LocalDate.now()) -> "A data de nascimento não pode estar no futuro."
            else -> null
        }
        val weight = Formats.parseDecimal(f.birthWeight)
        val length = Formats.parseDecimal(f.birthLength)
        val head = Formats.parseDecimal(f.birthHead)
        val measureError = if ((f.birthWeight.isNotBlank() && weight == null) ||
            (f.birthLength.isNotBlank() && length == null) ||
            (f.birthHead.isNotBlank() && head == null)
        ) {
            "Confira os números do nascimento (use vírgula, ex.: 3,250)."
        } else {
            null
        }
        _form.update { it.copy(nameError = nameError, dateError = dateError, measureError = measureError) }
        if (nameError != null || dateError != null || measureError != null) return
        val birthDate = f.birthDate!!

        persist {
            val baby = Baby(
                id = recordId ?: 0,
                name = f.name.trim(),
                birthDate = birthDate,
                birthTime = f.birthTime,
                sex = f.sex,
                bloodType = f.bloodType,
                notes = f.notes.orNullIfBlank(),
                colorIndex = f.colorIndex,
            )
            val id = container.babyRepository.save(baby)
            if (!isEdit) {
                listOf(
                    MeasurementType.WEIGHT to weight,
                    MeasurementType.HEIGHT to length,
                    MeasurementType.HEAD_CIRCUMFERENCE to head,
                ).forEach { (type, value) ->
                    if (value != null) {
                        container.recordRepository.saveGrowth(
                            GrowthMeasurement(babyId = id, date = birthDate, type = type, value = value, place = "Nascimento"),
                        )
                    }
                }
                if (f.addSuggestedVaccines) container.recordRepository.addSuggestedVaccines(baby.copy(id = id))
            }
            SavedRef(id, id)
        }
    }
}

@Composable
fun BabyFormScreen(args: FormArgs, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: BabyFormViewModel = viewModel { BabyFormViewModel(container, args.id) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(status.done) { if (status.done) onDone() }

    FormScaffold(
        title = if (viewModel.isEdit) "Dados do bebê" else "Cadastrar bebê",
        status = status,
        onBack = onDone,
        onSave = viewModel::save,
    ) {
        if (!viewModel.isEdit) InfoCard(Texts.WELCOME)
        TextInput(
            value = form.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v, nameError = null) } },
            label = "Nome ou apelido",
            error = form.nameError,
            capitalization = KeyboardCapitalization.Words,
        )
        DateField(
            label = "Data de nascimento",
            value = form.birthDate,
            onValueChange = { d -> viewModel.update { it.copy(birthDate = d, dateError = null) } },
            error = form.dateError,
        )
        TimeField(
            label = "Hora do nascimento (opcional)",
            value = form.birthTime,
            onValueChange = { t -> viewModel.update { it.copy(birthTime = t) } },
        )
        ChoiceChips(
            title = "Sexo (opcional)",
            options = SexOption.entries,
            selected = SexOption.entries.firstOrNull { it.sex == form.sex } ?: SexOption.NONE,
            optionLabel = { it.label },
            onSelect = { option -> viewModel.update { it.copy(sex = option.sex) } },
        )
        DropdownField(
            label = "Tipo sanguíneo (opcional)",
            options = BLOOD_TYPES,
            selected = form.bloodType,
            optionLabel = { it },
            onSelect = { v -> viewModel.update { it.copy(bloodType = v) } },
            noneLabel = "Não sei / não informar",
        )
        if (!viewModel.isEdit) {
            ExpandableSection(title = "Mais dados do nascimento", subtitle = "Peso, comprimento e perímetro cefálico") {
                DecimalInput(
                    value = form.birthWeight,
                    onValueChange = { v -> viewModel.update { it.copy(birthWeight = v, measureError = null) } },
                    label = "Peso ao nascer",
                    suffix = "kg",
                    supporting = "Ex.: 3,250",
                )
                DecimalInput(
                    value = form.birthLength,
                    onValueChange = { v -> viewModel.update { it.copy(birthLength = v, measureError = null) } },
                    label = "Comprimento ao nascer",
                    suffix = "cm",
                )
                DecimalInput(
                    value = form.birthHead,
                    onValueChange = { v -> viewModel.update { it.copy(birthHead = v, measureError = null) } },
                    label = "Perímetro cefálico ao nascer",
                    suffix = "cm",
                    error = form.measureError,
                )
            }
            SwitchRow(
                label = "Adicionar calendário de vacinas sugerido",
                supporting = "Cria as vacinas previstas com base na data de nascimento. Você pode editar depois.",
                checked = form.addSuggestedVaccines,
                onCheckedChange = { v -> viewModel.update { it.copy(addSuggestedVaccines = v) } },
            )
        }
        TextInput(
            value = form.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Observações (opcional)",
            singleLine = false,
            minLines = 2,
        )
        BabyColorPicker(
            selected = form.colorIndex,
            onSelect = { idx -> viewModel.update { it.copy(colorIndex = idx) } },
        )
        PhotoAttachmentSection(viewModel.photos, title = "Foto do bebê", hint = "A última foto adicionada aparece no perfil.")
        if (viewModel.isEdit) {
            InfoCard("Para excluir este bebê e todos os registros, vá em Ajustes.")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BabyColorPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Cor do tema",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BabyPalette.forEachIndexed { index, entry ->
                val isSelected = index == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(entry.primaryContainer)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, entry.primary, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            onClickLabel = entry.label,
                            role = Role.RadioButton,
                        ) { onSelect(index) }
                        .semantics { contentDescription = "${entry.label}${if (isSelected) ", selecionada" else ""}" },
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = entry.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
