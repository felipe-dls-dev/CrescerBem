package br.com.crescerbem.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.crescerbem.AppContainer
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.repo.ThemeMode
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.Texts
import br.com.crescerbem.ui.components.ChoiceChips
import br.com.crescerbem.ui.components.ConfirmDialog
import br.com.crescerbem.ui.components.EmptyHint
import br.com.crescerbem.ui.components.ExpandableSection
import br.com.crescerbem.ui.components.InfoCard
import br.com.crescerbem.ui.components.LocalAppContainer
import br.com.crescerbem.ui.components.SectionTitle
import br.com.crescerbem.ui.components.SubScreenScaffold
import br.com.crescerbem.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    fun setReminders(enabled: Boolean) {
        container.reminderStore.setEnabled(enabled)
        viewModelScope.launch { container.reminderSync.sync() }
    }

    val babies: StateFlow<List<Baby>> = container.babyRepository.babies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun deleteBaby(baby: Baby) {
        viewModelScope.launch {
            container.babyRepository.delete(baby)
            container.reminderSync.sync()
            _message.value = "${baby.name} e todos os registros foram apagados."
        }
    }

    fun eraseEverything() {
        viewModelScope.launch {
            container.dataEraser.eraseEverything()
            container.reminderSync.sync()
            _message.value = "Todos os dados foram apagados deste aparelho."
        }
    }
}

/** Exclusões pedem confirmação em duas etapas. */
private sealed interface PendingDeletion {
    data class OneBaby(val baby: Baby, val step: Int) : PendingDeletion
    data class Everything(val step: Int) : PendingDeletion
}

@Composable
fun SettingsScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val themeMode by container.themeStore.mode.collectAsStateWithLifecycle()
    val remindersOn by container.reminderStore.enabled.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingDeletion?>(null) }
    // Android 13+ pede permissão para notificar; só ligamos os lembretes se ela for concedida.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setReminders(granted)
    }

    SubScreenScaffold(title = "Ajustes", onBack = onBack) {
        message?.let { InfoCard(it) }

        SectionTitle("Bebês")
        if (babies.isEmpty()) EmptyHint("Nenhum bebê cadastrado.")
        babies.forEach { baby ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(baby.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Nascimento: ${Formats.date(baby.birthDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { navigate(Routes.form(RecordType.BABY, id = baby.id)) },
                    modifier = Modifier.semantics { contentDescription = "Editar dados de ${baby.name}" },
                ) { Text("Editar") }
                TextButton(
                    onClick = { pending = PendingDeletion.OneBaby(baby, 1) },
                    modifier = Modifier.semantics { contentDescription = "Excluir ${baby.name}" },
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        OutlinedButton(onClick = { navigate(Routes.form(RecordType.BABY)) }) { Text("Adicionar bebê") }

        HorizontalDivider()
        SectionTitle("Aparência")
        ChoiceChips(
            options = ThemeMode.entries,
            selected = themeMode,
            optionLabel = { it.label },
            onSelect = container.themeStore::set,
        )
        Text(
            "Automático segue o modo claro ou escuro do celular. O botão de sol e lua no topo também alterna.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()
        SectionTitle("Lembretes")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Avisar sobre consultas e vacinas",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = remindersOn,
                onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setReminders(on)
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Lembretes de consultas e vacinas" },
            )
        }
        Text(
            "Você recebe um aviso 1 dia e 2 horas antes de cada consulta, e no dia de cada vacina prevista. " +
                "Por privacidade, o aviso não mostra nomes nem detalhes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()
        SectionTitle("Dados e privacidade")
        InfoCard(Texts.PRIVACY_SUMMARY, icon = Icons.Filled.Lock)
        InfoCard(Texts.DATA_LOSS_WARNING)
        Button(
            onClick = { pending = PendingDeletion.Everything(1) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Apagar todos os dados") }

        HorizontalDivider()
        ExpandableSection(title = "Sobre o Crescer Bem") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Versão 0.1.0", style = MaterialTheme.typography.bodyMedium)
                Text(Texts.DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                Text(Texts.VACCINE_NOTE, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Em caso de emergência, ligue 192 (SAMU).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    when (val p = pending) {
        is PendingDeletion.OneBaby -> if (p.step == 1) {
            ConfirmDialog(
                title = "Excluir ${p.baby.name}?",
                text = "Todos os registros e fotos de ${p.baby.name} serão apagados deste aparelho.",
                confirmLabel = "Continuar",
                onConfirm = { pending = p.copy(step = 2) },
                onDismiss = { pending = null },
            )
        } else {
            ConfirmDialog(
                title = "Tem certeza?",
                text = "Essa ação não pode ser desfeita. Se quiser guardar uma cópia, gere um relatório em PDF antes.",
                confirmLabel = "Excluir definitivamente",
                onConfirm = {
                    viewModel.deleteBaby(p.baby)
                    pending = null
                },
                onDismiss = { pending = null },
            )
        }
        is PendingDeletion.Everything -> if (p.step == 1) {
            ConfirmDialog(
                title = "Apagar todos os dados?",
                text = "Todos os bebês, registros, fotos e relatórios serão apagados deste aparelho.",
                confirmLabel = "Continuar",
                onConfirm = { pending = p.copy(step = 2) },
                onDismiss = { pending = null },
            )
        } else {
            ConfirmDialog(
                title = "Tem certeza?",
                text = "Essa ação não pode ser desfeita.",
                confirmLabel = "Apagar tudo",
                onConfirm = {
                    viewModel.eraseEverything()
                    pending = null
                },
                onDismiss = { pending = null },
            )
        }
        null -> Unit
    }
}
