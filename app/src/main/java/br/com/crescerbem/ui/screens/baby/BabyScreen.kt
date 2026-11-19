package br.com.crescerbem.ui.screens.baby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.BabyAge
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.decimals
import br.com.crescerbem.domain.label
import br.com.crescerbem.domain.toTimelineItem
import br.com.crescerbem.domain.unit
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LoadingBox
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.MiniLineChart
import br.com.crescerbem.ui.components.TimelineRow
import br.com.crescerbem.ui.navigation.Routes
import br.com.crescerbem.ui.theme.CategoryColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

data class BabyState(
    val loading: Boolean = true,
    val baby: Baby? = null,
    val photo: File? = null,
    val growth: List<GrowthMeasurement> = emptyList(),
    val moments: List<Moment> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class BabyViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<BabyState> = container.babyRepository.activeBaby.flatMapLatest { baby ->
        if (baby == null) {
            flowOf(BabyState(loading = false))
        } else {
            combine(
                container.recordRepository.observeGrowth(baby.id),
                container.recordRepository.observeMoments(baby.id),
                container.photoRepository.observeFor(RecordType.BABY, baby.id),
            ) { growth, moments, photos ->
                BabyState(
                    loading = false,
                    baby = baby,
                    photo = photos.lastOrNull()?.let { container.photoStorage.fileFor(it.fileName) },
                    growth = growth,
                    moments = moments,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BabyState())
}

@Composable
fun BabyScreen(navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: BabyViewModel = viewModel { BabyViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox()
        return
    }
    val baby = state.baby
    if (baby == null) {
        Column(Modifier.padding(16.dp)) {
            EmptyHint(
                "Cadastre o bebê para acompanhar o crescimento.",
                actionLabel = "Cadastrar bebê",
                onAction = { navigate(Routes.form(RecordType.BABY)) },
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfileCard(baby, state.photo, onEdit = { navigate(Routes.form(RecordType.BABY, id = baby.id)) }) }
        MeasurementType.entries.forEach { type ->
            item(key = type.name) {
                GrowthSection(
                    type = type,
                    items = state.growth.filter { it.type == type },
                    onAdd = { navigate(Routes.form(RecordType.GROWTH, date = LocalDate.now(), preset = type.name)) },
                    onOpen = { navigate(Routes.form(RecordType.GROWTH, id = it.id)) },
                )
            }
        }
        item {
            ExpandableSection(
                title = "Momentos",
                subtitle = if (state.moments.isEmpty()) "Nenhum ainda" else "${state.moments.size} momento(s)",
                icon = Icons.Filled.AutoAwesome,
                iconTint = CategoryColors.moment,
            ) {
                if (state.moments.isEmpty()) {
                    EmptyHint("Guarde aqui o primeiro sorriso, o primeiro banho e outros marcos.")
                }
                state.moments.forEach { moment ->
                    TimelineRow(
                        moment.toTimelineItem(),
                        showDate = true,
                        onClick = { navigate(Routes.form(RecordType.MOMENT, id = moment.id)) },
                    )
                }
                OutlinedButton(onClick = { navigate(Routes.form(RecordType.MOMENT, date = LocalDate.now())) }) {
                    Text("Novo momento")
                }
            }
        }
        item {
            InfoCard(
                "O gráfico mostra só a evolução dos valores que você registrou. " +
                    "Converse com o pediatra sobre o crescimento do bebê.",
            )
        }
    }
}

@Composable
private fun ProfileCard(baby: Baby, photo: File?, onEdit: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Foto de ${baby.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp),
                    )
                } else {
                    Icon(Icons.Filled.ChildCare, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(baby.name, style = MaterialTheme.typography.headlineSmall)
                Text(BabyAge.describe(baby.birthDate, LocalDate.now()), style = MaterialTheme.typography.bodyLarge)
                val born = buildString {
                    append("Nasceu em ${Formats.date(baby.birthDate)}")
                    baby.birthTime?.let { append(" às ${Formats.time(it)}") }
                }
                Text(born, style = MaterialTheme.typography.bodyMedium)
                val extra = listOfNotNull(baby.sex?.label, baby.bloodType?.let { "Tipo sanguíneo $it" })
                if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onEdit, modifier = Modifier.padding(top = 4.dp)) { Text("Editar dados") }
            }
        }
    }
}

private val MeasurementType.icon: ImageVector
    get() = when (this) {
        MeasurementType.WEIGHT -> Icons.Filled.MonitorWeight
        MeasurementType.HEIGHT -> Icons.Filled.Straighten
        MeasurementType.HEAD_CIRCUMFERENCE -> Icons.Filled.Face
    }

@Composable
private fun GrowthSection(
    type: MeasurementType,
    items: List<GrowthMeasurement>,
    onAdd: () -> Unit,
    onOpen: (GrowthMeasurement) -> Unit,
) {
    val latest = items.firstOrNull()
    ExpandableSection(
        title = type.label,
        subtitle = latest?.let { "${Formats.measurement(type, it.value)} em ${Formats.date(it.date)}" } ?: "Sem registros",
        icon = type.icon,
        iconTint = CategoryColors.growth,
        stateKey = "growth-${type.name}",
    ) {
        if (items.size >= 2) {
            val chronological = items.sortedBy { it.date }
            val min = chronological.minOf { it.value }
            val max = chronological.maxOf { it.value }
            MiniLineChart(
                points = chronological.map { it.date to it.value },
                color = CategoryColors.growth,
                description = "Gráfico de ${type.label.lowercase()}: de ${Formats.measurement(type, chronological.first().value)} " +
                    "em ${Formats.date(chronological.first().date)} para ${Formats.measurement(type, chronological.last().value)} " +
                    "em ${Formats.date(chronological.last().date)}.",
                minLabel = "Menor: ${Formats.measurement(type, min)}",
                maxLabel = "Maior: ${Formats.measurement(type, max)}",
                startLabel = Formats.shortDate(chronological.first().date),
                endLabel = Formats.shortDate(chronological.last().date),
            )
        }
        if (items.isEmpty()) EmptyHint("Nenhuma medida de ${type.label.lowercase()} ainda.")
        items.forEachIndexed { index, measurement ->
            val previous = items.getOrNull(index + 1)
            ListItem(
                modifier = Modifier.clickable(onClickLabel = "Abrir medida") { onOpen(measurement) },
                headlineContent = { Text(Formats.measurement(type, measurement.value)) },
                supportingContent = {
                    Text(listOfNotNull(Formats.date(measurement.date), measurement.place).joinToString(" · "))
                },
                trailingContent = previous?.let { { Text(delta(type, measurement.value - it.value)) } },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        OutlinedButton(onClick = onAdd) { Text("Nova medida de ${type.label.lowercase()}") }
    }
}

private fun delta(type: MeasurementType, value: Double): String {
    val sign = if (value > 0) "+" else if (value < 0) "−" else ""
    return "$sign${Formats.decimal(abs(value), type.decimals)} ${type.unit}"
}
