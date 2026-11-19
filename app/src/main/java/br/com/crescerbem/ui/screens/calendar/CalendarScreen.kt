package br.com.crescerbem.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.CalendarGrid
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.TimelineItem
import br.com.crescerbem.domain.label
import br.com.crescerbem.domain.pluralLabel
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.LoadingBox
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.RecordTypePickerSheet
import br.com.crescerbem.ui.components.icon
import br.com.crescerbem.ui.components.TimelineRow
import br.com.crescerbem.ui.components.color
import br.com.crescerbem.ui.components.pickableRecordTypes
import br.com.crescerbem.ui.navigation.Routes
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
import java.time.YearMonth

data class CalendarState(
    val loading: Boolean = true,
    val baby: Baby? = null,
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val filters: Set<RecordType> = pickableRecordTypes.toSet(),
    val markers: Map<LocalDate, Set<RecordType>> = emptyMap(),
    val dayItems: List<TimelineItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(container: AppContainer) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())
    private val filters = MutableStateFlow(pickableRecordTypes.toSet())
    private val activeBaby = container.babyRepository.activeBaby

    private val monthItems = combine(activeBaby, month) { baby, m -> baby to m }.flatMapLatest { (baby, m) ->
        if (baby == null) flowOf(emptyList()) else container.timelineRepository.observeRange(baby.id, m.atDay(1), m.atEndOfMonth())
    }

    val state: StateFlow<CalendarState> =
        combine(activeBaby, month, selected, filters, monthItems) { baby, m, day, types, items ->
            val visible = items.filter { it.type in types }
            CalendarState(
                loading = false,
                baby = baby,
                month = m,
                selected = day,
                filters = types,
                markers = CalendarGrid.markersByDay(visible),
                dayItems = visible.filter { it.date == day },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun previousMonth() = showMonth(month.value.minusMonths(1))
    fun nextMonth() = showMonth(month.value.plusMonths(1))
    fun goToToday() {
        month.value = YearMonth.now()
        selected.value = LocalDate.now()
    }

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun toggleFilter(type: RecordType) {
        filters.update { current ->
            if (type in current) {
                (current - type).ifEmpty { current }
            } else {
                current + type
            }
        }
    }

    private fun showMonth(target: YearMonth) {
        month.value = target
        val today = LocalDate.now()
        selected.value = if (YearMonth.from(today) == target) today else target.atDay(1)
    }
}

/** Ordem dos cards de tipo na lista do dia. */
private val DAY_GROUP_ORDER = listOf(
    RecordType.MEDICATION,
    RecordType.HEALTH_EVENT,
    RecordType.APPOINTMENT,
    RecordType.VACCINE,
    RecordType.FEEDING,
    RecordType.GROWTH,
    RecordType.MOMENT,
    RecordType.BABY,
)

@Composable
fun CalendarScreen(navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: CalendarViewModel = viewModel { CalendarViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    if (state.loading) {
        LoadingBox()
        return
    }
    val baby = state.baby
    if (baby == null) {
        Column(Modifier.padding(16.dp)) {
            EmptyHint(
                "Cadastre o bebê para usar o calendário.",
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
        item {
            MonthHeader(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToToday,
            )
        }
        item { FilterRow(state.filters, viewModel::toggleFilter) }
        item {
            MonthGrid(
                month = state.month,
                selected = state.selected,
                birthDate = baby.birthDate,
                markers = state.markers,
                onSelect = viewModel::select,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formats.longDate(state.selected),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Button(onClick = { pickerOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("Adicionar")
                }
            }
        }
        if (state.selected == baby.birthDate) {
            item { Text("Dia do nascimento de ${baby.name}.", style = MaterialTheme.typography.bodyMedium) }
        }
        if (state.dayItems.isEmpty()) {
            item { EmptyHint("Nada registrado neste dia. Toque em Adicionar para criar um registro.") }
        } else {
            val byType = state.dayItems.groupBy { it.type }
            DAY_GROUP_ORDER.filter { it in byType }.forEach { type ->
                val list = byType.getValue(type)
                item(key = "group-$type") {
                    ExpandableSection(
                        title = type.pluralLabel,
                        subtitle = if (list.size == 1) "1 registro" else "${list.size} registros",
                        icon = type.icon,
                        iconTint = type.color,
                        initiallyExpanded = true,
                        stateKey = "calendar-day-$type",
                    ) {
                        list.forEach { item ->
                            TimelineRow(item, onClick = { navigate(Routes.form(item.type, id = item.id)) })
                        }
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        RecordTypePickerSheet(
            subtitle = "Para ${Formats.longDate(state.selected)}",
            onDismiss = { pickerOpen = false },
            onPick = { type ->
                pickerOpen = false
                navigate(Routes.form(type, date = state.selected))
            },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(
            Formats.monthYear(month),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
        TextButton(onClick = onToday) { Text("Hoje") }
    }
}

@Composable
private fun FilterRow(filters: Set<RecordType>, onToggle: (RecordType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pickableRecordTypes) { type ->
            FilterChip(
                selected = type in filters,
                onClick = { onToggle(type) },
                label = { Text(type.label) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(type.color),
                    )
                },
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    birthDate: LocalDate,
    markers: Map<LocalDate, Set<RecordType>>,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val weekdayNames = CalendarGrid.weekdayNames()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            CalendarGrid.weekdayInitials().forEachIndexed { index, initial ->
                Text(
                    initial,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = weekdayNames[index] },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        CalendarGrid.cells(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selected,
                                isBirthday = date == birthDate,
                                types = markers[date].orEmpty(),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isBirthday: Boolean,
    types: Set<RecordType>,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val description = buildString {
        append("${date.dayOfMonth} de ${Formats.monthYear(java.time.YearMonth.from(date)).lowercase()}")
        if (isToday) append(", hoje")
        if (isBirthday) append(", dia do nascimento")
        if (types.isEmpty()) append(", sem registros") else append(", com ${types.joinToString(", ") { it.pluralLabel.lowercase() }}")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(colors.primary)
                        isToday -> Modifier.border(2.dp, colors.primary, CircleShape)
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colors.onPrimary else colors.onSurface,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(10.dp).padding(top = 2.dp),
        ) {
            if (isBirthday) {
                Icon(Icons.Filled.Cake, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(8.dp))
            }
            types.take(if (isBirthday) 3 else 4).forEach { type ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(type.color),
                )
            }
        }
    }
}
