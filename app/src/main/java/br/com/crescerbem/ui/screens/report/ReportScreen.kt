package br.com.crescerbem.ui.screens.report

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.ReportBuilder
import br.com.crescerbem.domain.ReportRequest
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.countFor
import br.com.crescerbem.domain.pluralLabel
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.DateField
import br.com.crescerbem.ui.components.DisclaimerCard
import br.com.crescerbem.ui.components.DropdownField
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.FieldTitle
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.SubScreenScaffold
import br.com.crescerbem.ui.components.SwitchRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

enum class PeriodPreset(val label: String) {
    LAST_7("Últimos 7 dias"),
    LAST_30("Últimos 30 dias"),
    SINCE_BIRTH("Desde o nascimento"),
    CUSTOM("Personalizado"),
}

data class ReportUiState(
    val babies: List<Baby> = emptyList(),
    val baby: Baby? = null,
    val preset: PeriodPreset = PeriodPreset.LAST_30,
    val start: LocalDate = LocalDate.now().minusDays(29),
    val end: LocalDate = LocalDate.now(),
    val types: Set<RecordType> = ReportBuilder.reportableTypes.toSet(),
    val includePhotos: Boolean = false,
    val counts: Map<RecordType, Int>? = null,
    val generating: Boolean = false,
    val result: File? = null,
    val error: String? = null,
) {
    val periodError: String? get() = if (end.isBefore(start)) "O fim precisa ser depois do início." else null
    val canGenerate: Boolean get() = baby != null && types.isNotEmpty() && periodError == null && !generating
}

class ReportViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()
    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            container.babyRepository.babies.collectLatest { babies ->
                val activeId = _state.value.baby?.id
                val active = babies.firstOrNull { it.id == activeId }
                    ?: container.babyRepository.activeBaby.first()
                _state.update { it.copy(babies = babies, baby = active) }
                applyPreset(_state.value.preset)
            }
        }
    }

    fun selectBaby(baby: Baby?) {
        _state.update { it.copy(baby = baby, result = null) }
        applyPreset(_state.value.preset)
    }

    fun selectPreset(preset: PeriodPreset) = applyPreset(preset)

    private fun applyPreset(preset: PeriodPreset) {
        val today = LocalDate.now()
        _state.update { s ->
            val (start, end) = when (preset) {
                PeriodPreset.LAST_7 -> today.minusDays(6) to today
                PeriodPreset.LAST_30 -> today.minusDays(29) to today
                PeriodPreset.SINCE_BIRTH -> (s.baby?.birthDate ?: today) to today
                PeriodPreset.CUSTOM -> s.start to s.end
            }
            s.copy(preset = preset, start = start, end = end, result = null)
        }
        refreshCounts()
    }

    fun setStart(date: LocalDate) {
        _state.update { it.copy(preset = PeriodPreset.CUSTOM, start = date, result = null) }
        refreshCounts()
    }

    fun setEnd(date: LocalDate) {
        _state.update { it.copy(preset = PeriodPreset.CUSTOM, end = date, result = null) }
        refreshCounts()
    }

    fun toggleType(type: RecordType) {
        _state.update { s ->
            s.copy(types = if (type in s.types) s.types - type else s.types + type, result = null)
        }
    }

    fun setIncludePhotos(value: Boolean) = _state.update { it.copy(includePhotos = value, result = null) }

    /** Conta os registros do período para a pré-visualização. */
    private fun refreshCounts() {
        countJob?.cancel()
        val s = _state.value
        val baby = s.baby ?: return
        if (s.periodError != null) {
            _state.update { it.copy(counts = null) }
            return
        }
        countJob = viewModelScope.launch {
            val data = container.timelineRepository.loadReportData(baby.id, s.start, s.end, includePhotos = false)
            _state.update { it.copy(counts = ReportBuilder.reportableTypes.associateWith { type -> data.countFor(type) }) }
        }
    }

    fun generate() {
        val s = _state.value
        val baby = s.baby ?: return
        if (!s.canGenerate) return
        _state.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val data = container.timelineRepository.loadReportData(baby.id, s.start, s.end, s.includePhotos)
                    val request = ReportRequest(baby, s.start, s.end, s.types, s.includePhotos)
                    val document = ReportBuilder.build(request, data, LocalDateTime.now())
                    container.reportFiles.fileFor(s.start, s.end).also {
                        container.pdfWriter.write(document, it)
                        container.reportFiles.keepOnly(it)
                    }
                }
                _state.update { it.copy(generating = false, result = file) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(generating = false, error = "Não foi possível gerar o PDF. Tente de novo.") }
            }
        }
    }
}

@Composable
fun ReportScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ReportViewModel = viewModel { ReportViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SubScreenScaffold(title = "Relatório em PDF", onBack = onBack) {
        if (state.babies.isEmpty()) {
            EmptyHint("Cadastre o bebê e faça alguns registros para gerar um relatório.")
            return@SubScreenScaffold
        }
        Text(
            "Escolha o período e o que entra no relatório. O PDF é criado neste aparelho.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.babies.size > 1) {
            DropdownField(
                label = "Bebê",
                options = state.babies,
                selected = state.baby,
                optionLabel = { it.name },
                onSelect = viewModel::selectBaby,
            )
        }
        ChoiceChips(
            title = "Período",
            options = PeriodPreset.entries,
            selected = state.preset,
            optionLabel = { it.label },
            onSelect = viewModel::selectPreset,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("De", state.start, viewModel::setStart, Modifier.weight(1f))
            DateField("Até", state.end, viewModel::setEnd, Modifier.weight(1f), error = state.periodError)
        }
        FieldTitle("O que incluir")
        ReportBuilder.reportableTypes.forEach { type ->
            val count = state.counts?.get(type)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = type in state.types, role = Role.Checkbox, onValueChange = { viewModel.toggleType(type) })
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = type in state.types, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(type.pluralLabel, modifier = Modifier.weight(1f))
                Text(
                    count?.let { "$it registro(s)" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SwitchRow(
            label = "Incluir miniaturas das fotos",
            supporting = "Desligado por padrão: o arquivo fica menor e mais discreto.",
            checked = state.includePhotos,
            onCheckedChange = viewModel::setIncludePhotos,
        )
        Button(
            onClick = viewModel::generate,
            enabled = state.canGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            if (state.generating) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gerar PDF")
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.result?.let { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Relatório pronto", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${Formats.date(state.start)} a ${Formats.date(state.end)}. " +
                            "O arquivo só sai do celular se você compartilhar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { context.startActivity(container.reportFiles.shareIntent(file)) }) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Compartilhar")
                        }
                        OutlinedButton(onClick = {
                            try {
                                context.startActivity(container.reportFiles.viewIntent(file))
                            } catch (e: ActivityNotFoundException) {
                                context.startActivity(container.reportFiles.shareIntent(file))
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir")
                        }
                    }
                }
            }
        }
        DisclaimerCard()
        InfoCard(Texts.REPORT_FOOTER)
    }
}
