package br.com.crescerbem.ui.screens.today

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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.domain.BabyAge
import br.com.crescerbem.domain.FeedingDaySummary
import br.com.crescerbem.domain.FeedingSummaries
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.TimelineItem
import br.com.crescerbem.domain.VaccineDisplayStatus
import br.com.crescerbem.domain.displayStatus
import br.com.crescerbem.domain.referenceDate
import br.com.crescerbem.domain.title
import br.com.crescerbem.ui.components.DisclaimerCard
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.LoadingBox
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.RecordTypeBadge
import br.com.crescerbem.ui.components.StatTile
import br.com.crescerbem.ui.components.StatusPill
import br.com.crescerbem.ui.components.TimelineRow
import br.com.crescerbem.ui.navigation.Routes
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.screens.vaccines.statusColor
import br.com.crescerbem.ui.screens.vaccines.statusIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

data class TodayState(
    val loading: Boolean = true,
    val baby: Baby? = null,
    val today: LocalDate = LocalDate.now(),
    val todayItems: List<TimelineItem> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val vaccinesDue: List<Vaccine> = emptyList(),
    val feeding: FeedingDaySummary = FeedingDaySummary(),
    val lastWeight: GrowthMeasurement? = null,
    val lastHeight: GrowthMeasurement? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(container: AppContainer) : ViewModel() {
    private val records = container.recordRepository
    private val timeline = container.timelineRepository

    val state: StateFlow<TodayState> = container.babyRepository.activeBaby.flatMapLatest { baby ->
        if (baby == null) {
            flowOf(TodayState(loading = false))
        } else {
            val today = LocalDate.now()
            combine(
                timeline.observeRange(baby.id, today, today),
                records.observeUpcomingAppointments(baby.id, today.atStartOfDay(), limit = 5),
                records.observeVaccines(baby.id),
                records.observeFeedingsOn(baby.id, today),
                records.observeGrowth(baby.id),
            ) { items, appointments, vaccines, feedings, growth ->
                TodayState(
                    loading = false,
                    baby = baby,
                    today = today,
                    todayItems = items,
                    appointments = appointments,
                    vaccinesDue = vaccinesDue(vaccines, today),
                    feeding = FeedingSummaries.summarize(feedings),
                    lastWeight = growth.firstOrNull { it.type == MeasurementType.WEIGHT },
                    lastHeight = growth.firstOrNull { it.type == MeasurementType.HEIGHT },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    /** Vacinas atrasadas, pendentes ou previstas para os próximos 15 dias. */
    private fun vaccinesDue(vaccines: List<Vaccine>, today: LocalDate): List<Vaccine> =
        vaccines.filter {
            when (it.displayStatus(today)) {
                VaccineDisplayStatus.OVERDUE, VaccineDisplayStatus.PENDING -> true
                VaccineDisplayStatus.PLANNED -> it.scheduledDate?.let { d -> !d.isAfter(today.plusDays(15)) } == true
                else -> false
            }
        }.sortedBy { it.referenceDate }.take(6)
}

@Composable
fun TodayScreen(navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TodayViewModel = viewModel { TodayViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox()
        return
    }
    val baby = state.baby
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (baby == null) {
            item { WelcomeCard(onStart = { navigate(Routes.form(RecordType.BABY)) }) }
            return@LazyColumn
        }
        item { BabySummaryCard(state, onOpenGrowth = { type -> navigate(Routes.form(RecordType.GROWTH, preset = type.name)) }) }
        item {
            ExpandableSection(
                title = "Próximos compromissos",
                subtitle = upcomingSubtitle(state),
                icon = Icons.Filled.Event,
                initiallyExpanded = false,
            ) {
                if (state.appointments.isEmpty() && state.vaccinesDue.isEmpty()) {
                    EmptyHint("Nenhuma consulta ou vacina para os próximos dias. Toque em Registrar para agendar uma consulta.")
                }
                state.appointments.forEach { appointment ->
                    AppointmentRow(appointment, state.today) {
                        navigate(Routes.form(RecordType.APPOINTMENT, id = appointment.id))
                    }
                }
                state.vaccinesDue.forEach { vaccine ->
                    VaccineDueRow(vaccine, state.today) { navigate(Routes.form(RecordType.VACCINE, id = vaccine.id)) }
                }
            }
        }
        item {
            ExpandableSection(
                title = "Registros de hoje",
                subtitle = if (state.todayItems.isEmpty()) "Nada ainda" else "${state.todayItems.size} registro(s)",
                icon = Icons.Filled.Today,
                initiallyExpanded = false,
            ) {
                if (state.todayItems.isEmpty()) {
                    EmptyHint("Nada registrado hoje ainda. Toque em Registrar para começar.")
                }
                state.todayItems.forEach { item ->
                    TimelineRow(item, onClick = { navigate(Routes.form(item.type, id = item.id)) })
                }
            }
        }
        item {
            FeedingTodaySection(
                summary = state.feeding,
                onQuickAdd = { type -> navigate(Routes.form(RecordType.FEEDING, date = state.today, preset = type.name)) },
            )
        }
        item { DisclaimerCard() }
    }
}

private fun upcomingSubtitle(state: TodayState): String {
    val total = state.appointments.size + state.vaccinesDue.size
    return if (total == 0) "Tudo em dia" else "$total item(ns)"
}

@Composable
private fun WelcomeCard(onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Boas-vindas ao Crescer Bem", style = MaterialTheme.typography.headlineSmall)
            Text(Texts.WELCOME, style = MaterialTheme.typography.bodyLarge)
            Text(Texts.PRIVACY_SUMMARY, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Cadastrar o bebê") }
        }
    }
}

@Composable
private fun BabySummaryCard(state: TodayState, onOpenGrowth: (MeasurementType) -> Unit) {
    val baby = state.baby ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Formats.longDate(state.today), style = MaterialTheme.typography.labelLarge)
            Text(
                BabyAge.sentence(baby.name, baby.birthDate, state.today),
                style = MaterialTheme.typography.titleLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatTile(
                    value = state.lastWeight?.let { Formats.measurement(it.type, it.value) } ?: "—",
                    label = state.lastWeight?.let { "Peso em ${Formats.shortDate(it.date)}" } ?: "Peso",
                    modifier = Modifier.clickable(onClickLabel = "Registrar peso") { onOpenGrowth(MeasurementType.WEIGHT) },
                )
                StatTile(
                    value = state.lastHeight?.let { Formats.measurement(it.type, it.value) } ?: "—",
                    label = state.lastHeight?.let { "Altura em ${Formats.shortDate(it.date)}" } ?: "Altura",
                    modifier = Modifier.clickable(onClickLabel = "Registrar altura") { onOpenGrowth(MeasurementType.HEIGHT) },
                )
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: Appointment, today: LocalDate, onClick: () -> Unit) {
    val date = appointment.dateTime.toLocalDate()
    val whenText = when (date) {
        today -> "Hoje às ${Formats.time(appointment.dateTime.toLocalTime())}"
        today.plusDays(1) -> "Amanhã às ${Formats.time(appointment.dateTime.toLocalTime())}"
        else -> Formats.dateTime(appointment.dateTime)
    }
    val past = appointment.dateTime.isBefore(LocalDateTime.now())
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { RecordTypeBadge(RecordType.APPOINTMENT) },
        overlineContent = { Text(RecordType.APPOINTMENT.label) },
        headlineContent = { Text(appointment.title) },
        supportingContent = {
            Text(if (past) "$whenText · Foi realizada? Toque para atualizar." else whenText)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun VaccineDueRow(vaccine: Vaccine, today: LocalDate, onClick: () -> Unit) {
    val status = vaccine.displayStatus(today)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { RecordTypeBadge(RecordType.VACCINE) },
        overlineContent = { Text(RecordType.VACCINE.label) },
        headlineContent = { Text(vaccine.title()) },
        supportingContent = { vaccine.scheduledDate?.let { Text("Prevista para ${Formats.date(it)}") } },
        trailingContent = { StatusPill(status.label, statusColor(status), icon = statusIcon(status)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedingTodaySection(summary: FeedingDaySummary, onQuickAdd: (FeedingType) -> Unit) {
    ExpandableSection(
        title = "Alimentação de hoje",
        subtitle = if (summary.isEmpty) "Nenhum registro" else "${summary.total} registro(s)",
        icon = Icons.Filled.Restaurant,
        initiallyExpanded = false,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatTile("${summary.breastfeedings}", "mamadas (${summary.breastMinutes} min)")
            StatTile("${summary.formulaMl} ml", "fórmula")
            StatTile("${summary.solidMeals}", "refeições")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onQuickAdd(FeedingType.BREAST_MILK) }) { Text("Mamou agora") }
            FilledTonalButton(onClick = { onQuickAdd(FeedingType.FORMULA) }) { Text("Fórmula") }
            FilledTonalButton(onClick = { onQuickAdd(FeedingType.SOLID_MEAL) }) { Text("Comida") }
        }
    }
}
