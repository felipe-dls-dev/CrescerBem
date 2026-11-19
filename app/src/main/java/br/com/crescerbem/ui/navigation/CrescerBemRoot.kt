package br.com.crescerbem.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.BabyAge
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.RecordTypePickerSheet
import br.com.crescerbem.ui.screens.appointments.AppointmentFormScreen
import br.com.crescerbem.ui.screens.baby.BabyFormScreen
import br.com.crescerbem.ui.screens.baby.BabyScreen
import br.com.crescerbem.ui.screens.calendar.CalendarScreen
import br.com.crescerbem.ui.screens.feeding.FeedingFormScreen
import br.com.crescerbem.ui.screens.feeding.FeedingScreen
import br.com.crescerbem.ui.screens.growth.GrowthFormScreen
import br.com.crescerbem.ui.screens.health.HealthEventFormScreen
import br.com.crescerbem.ui.screens.health.HealthScreen
import br.com.crescerbem.ui.screens.medications.MedicationFormScreen
import br.com.crescerbem.ui.screens.moments.MomentFormScreen
import br.com.crescerbem.ui.screens.report.ReportScreen
import br.com.crescerbem.ui.screens.settings.SettingsScreen
import br.com.crescerbem.ui.screens.today.TodayScreen
import br.com.crescerbem.ui.screens.vaccines.VaccineFormScreen
import br.com.crescerbem.ui.theme.BabyPalette
import br.com.crescerbem.ui.theme.LOGO_ASPECT_RATIO
import br.com.crescerbem.ui.theme.rememberLogoMark
import java.time.LocalDate

@Composable
fun CrescerBemRoot(
    container: AppContainer,
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        val navController = rememberNavController()
        val mainViewModel: MainViewModel = viewModel { MainViewModel(container.babyRepository) }
        val state by mainViewModel.state.collectAsStateWithLifecycle()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val isTopLevel = TopLevel.entries.any { it.route == currentRoute }
        var showPicker by rememberSaveable { mutableStateOf(false) }
        val navigate: (String) -> Unit = { route -> navController.navigate(route) }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LogoWatermark(Modifier.align(Alignment.Center))
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                if (isTopLevel) {
                    MainTopBar(
                        state = state,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onSelect = mainViewModel::select,
                        onAddBaby = { navigate(Routes.form(RecordType.BABY)) },
                        onEditBaby = { id -> navigate(Routes.form(RecordType.BABY, id = id)) },
                        onReport = { navigate(Routes.REPORT) },
                        onSettings = { navigate(Routes.SETTINGS) },
                    )
                }
            },
            bottomBar = {
                if (isTopLevel) BottomBar(navController, currentRoute)
            },
            floatingActionButton = {
                if (isTopLevel && state.active != null) {
                    ExtendedFloatingActionButton(
                        onClick = { showPicker = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Registrar") },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = TopLevel.TODAY.route,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                composable(TopLevel.TODAY.route) { TodayScreen(navigate) }
                composable(TopLevel.CALENDAR.route) { CalendarScreen(navigate) }
                composable(TopLevel.BABY.route) { BabyScreen(navigate) }
                composable(TopLevel.HEALTH.route) { HealthScreen(navigate) }
                composable(TopLevel.FEEDING.route) { FeedingScreen(navigate) }
                composable(Routes.REPORT) { ReportScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() }, navigate = navigate)
                }
                composable(
                    route = Routes.FORM_PATTERN,
                    arguments = listOf(
                        navArgument(Routes.ARG_TYPE) { type = NavType.StringType },
                        navArgument(Routes.ARG_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(Routes.ARG_DATE) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(Routes.ARG_PRESET) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    val args = FormArgs.from(entry.arguments)
                    // Evita voltar duas vezes (ex.: salvar e tocar em voltar ao mesmo tempo).
                    val onDone: () -> Unit = {
                        if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            navController.popBackStack()
                        }
                    }
                    FormHost(args, onDone)
                }
            }
        }
        }

        if (showPicker) {
            RecordTypePickerSheet(
                onDismiss = { showPicker = false },
                onPick = { type ->
                    showPicker = false
                    navigate(Routes.form(type, date = LocalDate.now()))
                },
            )
        }
    }
}

/** Logo do app bem suave ao fundo de todas as telas (os blocos ficam por cima). */
@Composable
private fun LogoWatermark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val logo = rememberLogoMark(heart = colors.primary, sprout = colors.background)
    Image(
        imageVector = logo,
        contentDescription = null,
        alpha = if (colors.background.luminance() < 0.5f) 0.10f else 0.08f,
        modifier = modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(LOGO_ASPECT_RATIO),
    )
}

@Composable
private fun FormHost(args: FormArgs, onDone: () -> Unit) {
    when (args.type) {
        RecordType.BABY -> BabyFormScreen(args, onDone)
        RecordType.MOMENT -> MomentFormScreen(args, onDone)
        RecordType.VACCINE -> VaccineFormScreen(args, onDone)
        RecordType.APPOINTMENT -> AppointmentFormScreen(args, onDone)
        RecordType.HEALTH_EVENT -> HealthEventFormScreen(args, onDone)
        RecordType.GROWTH -> GrowthFormScreen(args, onDone)
        RecordType.FEEDING -> FeedingFormScreen(args, onDone)
        RecordType.MEDICATION -> MedicationFormScreen(args, onDone)
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        TopLevel.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                // Indicador forte (cor do bebê) para a aba selecionada ficar bem visível.
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    state: MainState,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSelect: (Long) -> Unit,
    onAddBaby: () -> Unit,
    onEditBaby: (Long) -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            val baby = state.active
            if (baby == null) {
                Text("Crescer Bem")
            } else {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Trocar de bebê ou editar dados") { menuOpen = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(baby.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                BabyAge.describe(baby.birthDate, LocalDate.now()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        state.babies.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    onSelect(item.id)
                                    menuOpen = false
                                },
                                leadingIcon = {
                                    val palette = BabyPalette.getOrElse(item.colorIndex) { BabyPalette[0] }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (darkTheme) palette.primaryDark else palette.primary),
                                    )
                                },
                                trailingIcon = if (item.id == baby.id) {
                                    { Icon(Icons.Filled.Check, contentDescription = "Selecionado") }
                                } else {
                                    null
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Editar dados do bebê") },
                            onClick = {
                                menuOpen = false
                                onEditBaby(baby.id)
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Adicionar outro bebê") },
                            onClick = {
                                menuOpen = false
                                onAddBaby()
                            },
                            leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = if (darkTheme) "Usar modo claro" else "Usar modo escuro",
                )
            }
            IconButton(onClick = onReport) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Relatório em PDF")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
            }
        },
    )
}
