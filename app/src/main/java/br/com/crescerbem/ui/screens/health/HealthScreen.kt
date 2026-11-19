package br.com.crescerbem.ui.screens.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.domain.VaccineDisplayStatus
import br.com.crescerbem.domain.VaccineSchedule
import br.com.crescerbem.domain.displayStatus
import br.com.crescerbem.domain.isOverdue
import br.com.crescerbem.domain.label
import br.com.crescerbem.domain.referenceTime
import br.com.crescerbem.domain.toTimelineItem
import br.com.crescerbem.ui.components.DisclaimerCard
import br.com.crescerbem.ui.components.DropdownField
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LoadingBox
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.RecordTypeBadge
import br.com.crescerbem.ui.components.StatusPill
import br.com.crescerbem.ui.components.TimelineRow
import br.com.crescerbem.ui.navigation.Routes
import br.com.crescerbem.ui.screens.medications.PRESET_GIVEN
import br.com.crescerbem.ui.screens.vaccines.statusColor
import br.com.crescerbem.ui.screens.vaccines.statusIcon
import br.com.crescerbem.ui.theme.CategoryColors
import br.com.crescerbem.ui.theme.StatusColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class HealthState(
    val loading: Boolean = true,
    val baby: Baby? = null,
    val vaccines: List<Vaccine> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val events: List<HealthEvent> = emptyList(),
    val medications: List<MedicationDose> = emptyList(),
    val today: LocalDate = LocalDate.now(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(private val container: AppContainer) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<HealthState> = container.babyRepository.activeBaby.flatMapLatest { baby ->
        if (baby == null) {
            flowOf(HealthState(loading = false))
        } else {
            val records = container.recordRepository
            combine(
                records.observeVaccines(baby.id),
                records.observeAppointments(baby.id),
                records.observeHealthEvents(baby.id),
                records.observeMedications(baby.id),
            ) { vaccines, appointments, events, medications ->
                HealthState(false, baby, vaccines, appointments, events, medications, LocalDate.now())
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthState())

    fun addSuggestedVaccines() {
        val baby = state.value.baby ?: return
        viewModelScope.launch {
            val added = container.recordRepository.addSuggestedVaccines(baby)
            _message.value = if (added == 0) {
                "O calendário sugerido já está completo."
            } else {
                "$added vacinas previstas foram adicionadas."
            }
        }
    }

    /** Confirma a dose como administrada agora, sem abrir o formulário. */
    fun markGivenNow(dose: MedicationDose) {
        viewModelScope.launch {
            container.recordRepository.saveMedication(
                dose.copy(
                    status = MedicationStatus.GIVEN,
                    givenAt = LocalDateTime.now().withSecond(0).withNano(0),
                ),
            )
        }
    }

    /** Registra que a dose não foi realizada (o responsável não deu o medicamento). */
    fun markSkipped(dose: MedicationDose) {
        viewModelScope.launch {
            container.recordRepository.saveMedication(
                dose.copy(status = MedicationStatus.SKIPPED, givenAt = null),
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

private val TABS = listOf("Medicações", "Sintomas", "Consultas", "Vacinas")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HealthViewModel = viewModel { HealthViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(message) {
        if (message != null) {
            delay(5_000)
            viewModel.clearMessage()
        }
    }

    if (state.loading) {
        LoadingBox()
        return
    }
    if (state.baby == null) {
        Column(Modifier.padding(16.dp)) {
            EmptyHint(
                "Cadastre o bebê para registrar vacinas, consultas e sintomas.",
                actionLabel = "Cadastrar bebê",
                onAction = { navigate(Routes.form(RecordType.BABY)) },
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            TABS.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, maxLines = 1) })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                0 -> item { MedicationsTab(state, navigate, viewModel::markGivenNow, viewModel::markSkipped) }
                1 -> item { SymptomsTab(state, navigate) }
                2 -> item { AppointmentsTab(state, navigate) }
                else -> vaccinesTab(state, message, navigate, viewModel::addSuggestedVaccines)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.vaccinesTab(
    state: HealthState,
    message: String?,
    navigate: (String) -> Unit,
    onAddSuggested: () -> Unit,
) {
    item { InfoCard(Texts.VACCINE_NOTE) }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { navigate(Routes.form(RecordType.VACCINE, date = LocalDate.now())) }) { Text("Nova vacina") }
            OutlinedButton(onClick = onAddSuggested) { Text("Adicionar calendário sugerido") }
        }
    }
    if (message != null) item { InfoCard(message) }
    if (state.vaccines.isEmpty()) {
        item { EmptyHint("Nenhuma vacina registrada. Adicione o calendário sugerido para ver as próximas doses.") }
        return
    }
    val groups = state.vaccines.groupBy { it.recommendedAgeMonths }
        .toSortedMap(nullsLast(naturalOrder()))
    groups.forEach { (months, list) ->
        item(key = "vaccines-$months") {
            val statuses = list.map { it.displayStatus(state.today) }
            val applied = statuses.count { it == VaccineDisplayStatus.APPLIED }
            val overdue = statuses.count { it == VaccineDisplayStatus.OVERDUE }
            ExpandableSection(
                title = VaccineSchedule.ageGroupLabel(months),
                subtitle = buildString {
                    append("$applied de ${list.size} aplicadas")
                    if (overdue > 0) append(" · $overdue atrasada(s)")
                },
                icon = Icons.Filled.Vaccines,
                iconTint = if (overdue > 0) StatusColors.overdue else CategoryColors.vaccine,
                initiallyExpanded = false,
                stateKey = "vaccine-group-$months",
            ) {
                list.forEach { vaccine ->
                    VaccineRow(vaccine, state.today) { navigate(Routes.form(RecordType.VACCINE, id = vaccine.id)) }
                }
            }
        }
    }
}

@Composable
private fun VaccineRow(vaccine: Vaccine, today: LocalDate, onClick: () -> Unit) {
    val status = vaccine.displayStatus(today)
    val dateText = when (status) {
        VaccineDisplayStatus.APPLIED -> vaccine.appliedDate?.let { "Aplicada em ${Formats.date(it)}" }
        VaccineDisplayStatus.NOT_APPLICABLE -> null
        else -> vaccine.scheduledDate?.let { "Prevista para ${Formats.date(it)}" }
    }
    ListItem(
        modifier = Modifier.clickable(onClickLabel = "Abrir vacina", onClick = onClick),
        headlineContent = { Text(vaccine.name) },
        supportingContent = {
            Text(listOfNotNull(vaccine.dose, dateText).joinToString(" · "))
        },
        trailingContent = { StatusPill(status.label, statusColor(status), icon = statusIcon(status)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentsTab(state: HealthState, navigate: (String) -> Unit) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val upcoming = state.appointments.filter { it.status == AppointmentStatus.SCHEDULED }.sortedBy { it.dateTime }
    val history = state.appointments.filter { it.status != AppointmentStatus.SCHEDULED }.sortedByDescending { it.dateTime }
    val now = LocalDateTime.now()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { navigate(Routes.form(RecordType.APPOINTMENT, date = LocalDate.now())) }) { Text("Nova consulta") }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !showHistory,
                onClick = { showHistory = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Próximas (${upcoming.size})") }
            SegmentedButton(
                selected = showHistory,
                onClick = { showHistory = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Histórico (${history.size})") }
        }
        val list = if (showHistory) history else upcoming
        if (list.isEmpty()) {
            EmptyHint(
                if (showHistory) {
                    "Nenhuma consulta realizada ou cancelada ainda."
                } else {
                    "Nenhuma consulta agendada. Toque em Nova consulta para marcar a próxima."
                },
            )
        }
        list.forEach { appointment ->
            val past = appointment.status == AppointmentStatus.SCHEDULED && appointment.dateTime.isBefore(now)
            ListItem(
                modifier = Modifier.clickable { navigate(Routes.form(RecordType.APPOINTMENT, id = appointment.id)) },
                leadingContent = { RecordTypeBadge(RecordType.APPOINTMENT) },
                overlineContent = { Text(RecordType.APPOINTMENT.label) },
                headlineContent = { Text(appointment.title) },
                supportingContent = {
                    Text(
                        listOfNotNull(
                            Formats.dateTime(appointment.dateTime),
                            appointment.professional,
                            appointment.place,
                        ).joinToString(" · "),
                    )
                },
                trailingContent = when {
                    past -> {
                        { StatusPill("Confirmar", StatusColors.pending) }
                    }
                    appointment.status == AppointmentStatus.DONE -> {
                        { StatusPill(appointment.status.label, StatusColors.done) }
                    }
                    appointment.status == AppointmentStatus.CANCELED -> {
                        { StatusPill(appointment.status.label, StatusColors.neutral) }
                    }
                    else -> null
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationsTab(
    state: HealthState,
    navigate: (String) -> Unit,
    onGiven: (MedicationDose) -> Unit,
    onSkipped: (MedicationDose) -> Unit,
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val upcoming = state.medications.filter { it.status == MedicationStatus.SCHEDULED }.sortedBy { it.scheduledAt }
    val history = state.medications.filter { it.status != MedicationStatus.SCHEDULED }
        .sortedByDescending { it.referenceTime }
    val now = LocalDateTime.now()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(Texts.MEDICATION_NOTE)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { navigate(Routes.form(RecordType.MEDICATION, date = LocalDate.now())) }) {
                Text("Agendar medicamento")
            }
            OutlinedButton(
                onClick = {
                    navigate(Routes.form(RecordType.MEDICATION, date = LocalDate.now(), preset = PRESET_GIVEN))
                },
            ) { Text("Registrar dose dada") }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !showHistory,
                onClick = { showHistory = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Próximas (${upcoming.size})") }
            SegmentedButton(
                selected = showHistory,
                onClick = { showHistory = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Histórico (${history.size})") }
        }
        val list = if (showHistory) history else upcoming
        if (list.isEmpty()) {
            EmptyHint(
                if (showHistory) {
                    "Nenhuma dose registrada ainda."
                } else {
                    "Nenhuma dose agendada. Toque em Agendar medicamento para marcar os horários."
                },
            )
        }
        list.forEach { dose ->
            val overdue = dose.isOverdue(now)
            ListItem(
                modifier = Modifier.clickable { navigate(Routes.form(RecordType.MEDICATION, id = dose.id)) },
                leadingContent = { RecordTypeBadge(RecordType.MEDICATION) },
                overlineContent = { Text(Formats.dateTime(dose.referenceTime)) },
                headlineContent = { Text(listOfNotNull(dose.name, dose.dosage).joinToString(" · ")) },
                supportingContent = when {
                    overdue -> ({
                        Column {
                            StatusPill("Não registrado", StatusColors.pending)
                            dose.notes?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                    })
                    else -> dose.notes?.takeIf { it.isNotBlank() }?.let { { Text(it) } }
                },
                trailingContent = when {
                    dose.status == MedicationStatus.GIVEN ->
                        ({ StatusPill(dose.status.label, StatusColors.done) })
                    dose.status == MedicationStatus.SKIPPED ->
                        ({ StatusPill(dose.status.label, StatusColors.neutral) })
                    overdue -> ({
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { onGiven(dose) }) { Text("Realizado") }
                            OutlinedButton(onClick = { onSkipped(dose) }) { Text("Não realizado") }
                        }
                    })
                    else -> ({
                        OutlinedButton(onClick = { onGiven(dose) }) { Text("Realizar agora") }
                    })
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun SymptomsTab(state: HealthState, navigate: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<Symptom?>(null) }
    val events = state.events.filter { filter == null || filter in it.symptoms }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { navigate(Routes.form(RecordType.HEALTH_EVENT, date = LocalDate.now())) }) {
            Text("Registrar sintoma")
        }
        DropdownField(
            label = "Filtrar por sintoma",
            options = Symptom.entries,
            selected = filter,
            optionLabel = { it.label },
            onSelect = { filter = it },
            noneLabel = "Todos os sintomas",
        )
        if (events.isEmpty()) {
            EmptyHint("Nenhum sintoma registrado. Se notar algum sinal no bebê, toque em Registrar sintoma.")
        }
        events.forEach { event ->
            TimelineRow(
                event.toTimelineItem(),
                showDate = true,
                onClick = { navigate(Routes.form(RecordType.HEALTH_EVENT, id = event.id)) },
            )
        }
        DisclaimerCard()
    }
}
