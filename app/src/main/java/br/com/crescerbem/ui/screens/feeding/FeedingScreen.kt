package br.com.crescerbem.ui.screens.feeding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.db.FoodTried
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.FeedingDaySummary
import br.com.crescerbem.domain.FeedingSummaries
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.describe
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LoadingBox
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.StatTile
import br.com.crescerbem.ui.navigation.Routes
import br.com.crescerbem.ui.theme.CategoryColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class FeedingState(
    val loading: Boolean = true,
    val baby: Baby? = null,
    val day: LocalDate = LocalDate.now(),
    val feedings: List<Feeding> = emptyList(),
    val summary: FeedingDaySummary = FeedingDaySummary(),
    val foodsTried: List<FoodTried> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingViewModel(container: AppContainer) : ViewModel() {
    private val day = MutableStateFlow(LocalDate.now())

    val state: StateFlow<FeedingState> =
        combine(container.babyRepository.activeBaby, day) { baby, d -> baby to d }.flatMapLatest { (baby, d) ->
            if (baby == null) {
                flowOf(FeedingState(loading = false, day = d))
            } else {
                combine(
                    container.recordRepository.observeFeedingsOn(baby.id, d),
                    container.recordRepository.observeFoodsTried(baby.id),
                ) { feedings, foods ->
                    FeedingState(
                        loading = false,
                        baby = baby,
                        day = d,
                        feedings = feedings,
                        summary = FeedingSummaries.summarize(feedings),
                        foodsTried = foods,
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedingState())

    fun previousDay() = day.update { it.minusDays(1) }
    fun nextDay() = day.update { if (it.isBefore(LocalDate.now())) it.plusDays(1) else it }
}

/** Grupos exibidos como seções recolhíveis. */
private enum class FeedingGroup(val title: String, val types: Set<FeedingType>, val icon: ImageVector) {
    BREAST("Aleitamento materno", setOf(FeedingType.BREAST_MILK), Icons.Filled.Favorite),
    FORMULA("Fórmula", setOf(FeedingType.FORMULA), Icons.Filled.LocalDrink),
    SOLIDS("Introdução alimentar", setOf(FeedingType.FRUIT, FeedingType.SOLID_MEAL), Icons.Filled.Eco),
    OTHER("Água e outros", setOf(FeedingType.WATER, FeedingType.OTHER), Icons.Filled.WaterDrop),
}

@Composable
fun FeedingScreen(navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: FeedingViewModel = viewModel { FeedingViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox()
        return
    }
    if (state.baby == null) {
        Column(Modifier.padding(16.dp)) {
            EmptyHint(
                "Cadastre o bebê para registrar a alimentação.",
                actionLabel = "Cadastrar bebê",
                onAction = { navigate(Routes.form(RecordType.BABY)) },
            )
        }
        return
    }
    val add: (FeedingType) -> Unit = { type -> navigate(Routes.form(RecordType.FEEDING, date = state.day, preset = type.name)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { QuickButtons(add) }
        item {
            DaySelector(
                day = state.day,
                onPrevious = viewModel::previousDay,
                onNext = viewModel::nextDay,
                canGoNext = state.day.isBefore(LocalDate.now()),
            )
        }
        item { SummaryCard(state.summary) }
        FeedingGroup.entries.forEach { group ->
            val items = state.feedings.filter { it.type in group.types }
            item(key = group.name) {
                ExpandableSection(
                    title = group.title,
                    subtitle = if (items.isEmpty()) "Nada neste dia" else "${items.size} registro(s)",
                    icon = group.icon,
                    iconTint = MaterialTheme.colorScheme.primary,
                    initiallyExpanded = false,
                    stateKey = "feeding-${group.name}",
                ) {
                    if (items.isEmpty()) EmptyHint("Nenhum registro neste dia. Use os botões rápidos no topo para registrar.")
                    items.forEach { feeding ->
                        ListItem(
                            modifier = Modifier.clickable(onClickLabel = "Abrir registro de alimentação") {
                                navigate(Routes.form(RecordType.FEEDING, id = feeding.id))
                            },
                            headlineContent = { Text(feeding.type.label) },
                            supportingContent = feeding.describe().takeIf { it.isNotBlank() }?.let { { Text(it) } },
                            trailingContent = { Text(Formats.time(feeding.dateTime.toLocalTime())) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
        item {
            ExpandableSection(
                title = "Alimentos já experimentados",
                subtitle = if (state.foodsTried.isEmpty()) "Nenhum ainda" else "${state.foodsTried.size} alimento(s)",
                icon = Icons.Filled.Restaurant,
                iconTint = MaterialTheme.colorScheme.primary,
            ) {
                if (state.foodsTried.isEmpty()) {
                    EmptyHint("Os alimentos registrados na introdução alimentar aparecem aqui.")
                }
                state.foodsTried.forEach { food ->
                    Text(
                        "${food.food} — desde ${Formats.date(food.firstDateTime.toLocalDate())}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            InfoCard(
                "A introdução de outros alimentos costuma começar por volta dos 6 meses. " +
                    "Converse com o pediatra antes de mudar a alimentação do bebê.",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickButtons(onAdd: (FeedingType) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = { onAdd(FeedingType.BREAST_MILK) }) { Text("Mamou agora") }
        FilledTonalButton(onClick = { onAdd(FeedingType.FORMULA) }) { Text("Fórmula") }
        FilledTonalButton(onClick = { onAdd(FeedingType.FRUIT) }) { Text("Fruta") }
        FilledTonalButton(onClick = { onAdd(FeedingType.SOLID_MEAL) }) { Text("Papa salgada") }
        FilledTonalButton(onClick = { onAdd(FeedingType.WATER) }) { Text("Água") }
    }
}

@Composable
private fun DaySelector(day: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, canGoNext: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
        }
        Text(
            if (day == LocalDate.now()) "Hoje" else Formats.longDate(day),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo dia")
        }
    }
}

@Composable
private fun SummaryCard(summary: FeedingDaySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumo do dia", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatTile("${summary.breastfeedings}", "mamadas")
                StatTile("${summary.breastMinutes} min", "no peito")
                StatTile("${summary.formulaMl} ml", "fórmula")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatTile("${summary.waterMl} ml", "água")
                StatTile("${summary.solidMeals}", "refeições")
            }
            if (summary.newFoods.isNotEmpty()) {
                Text(
                    "Alimentos novos: ${summary.newFoods.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
