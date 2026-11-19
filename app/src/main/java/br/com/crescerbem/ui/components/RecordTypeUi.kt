package br.com.crescerbem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.domain.Formats
import br.com.crescerbem.domain.TimelineItem
import br.com.crescerbem.domain.label
import br.com.crescerbem.ui.theme.CategoryColors

val RecordType.icon: ImageVector
    get() = when (this) {
        RecordType.BABY -> Icons.Filled.ChildCare
        RecordType.MOMENT -> Icons.Filled.AutoAwesome
        RecordType.VACCINE -> Icons.Filled.Vaccines
        RecordType.APPOINTMENT -> Icons.Filled.Event
        RecordType.HEALTH_EVENT -> Icons.Filled.Sick
        RecordType.GROWTH -> Icons.Filled.MonitorWeight
        RecordType.FEEDING -> Icons.Filled.Restaurant
        RecordType.MEDICATION -> Icons.Filled.Medication
    }

val RecordType.color: Color
    get() = when (this) {
        RecordType.BABY -> CategoryColors.growth
        RecordType.MOMENT -> CategoryColors.moment
        RecordType.VACCINE -> CategoryColors.vaccine
        RecordType.APPOINTMENT -> CategoryColors.appointment
        RecordType.HEALTH_EVENT -> CategoryColors.health
        RecordType.GROWTH -> CategoryColors.growth
        RecordType.FEEDING -> CategoryColors.feeding
        RecordType.MEDICATION -> CategoryColors.medication
    }

val RecordType.hint: String
    get() = when (this) {
        RecordType.BABY -> "Dados do bebê"
        RecordType.MOMENT -> "Acontecimentos do dia a dia, com fotos"
        RecordType.VACCINE -> "Aplicada ou prevista"
        RecordType.APPOINTMENT -> "Agendada ou realizada"
        RecordType.HEALTH_EVENT -> "Febre, cólica e outros sinais"
        RecordType.GROWTH -> "Peso, altura e perímetro cefálico"
        RecordType.FEEDING -> "Mamadas, fórmula e comidinhas"
        RecordType.MEDICATION -> "Agendar ou registrar uma dose"
    }

/** Tipos que o responsável pode registrar pelo botão "Registrar". */
val pickableRecordTypes = listOf(
    RecordType.MOMENT,
    RecordType.FEEDING,
    RecordType.GROWTH,
    RecordType.HEALTH_EVENT,
    RecordType.VACCINE,
    RecordType.APPOINTMENT,
    RecordType.MEDICATION,
)

@Composable
fun RecordTypeBadge(type: RecordType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    // No modo escuro as cores de categoria são clareadas para o ícone continuar legível.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tint = if (dark) lerp(type.color, Color.White, 0.4f) else type.color
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(type.color.copy(alpha = if (dark) 0.28f else 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun TimelineRow(
    item: TimelineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
) {
    val whenText = when {
        showDate && item.time != null -> "${Formats.shortDate(item.date)} ${Formats.time(item.time)}"
        showDate -> Formats.shortDate(item.date)
        item.time != null -> Formats.time(item.time)
        else -> null
    }
    ListItem(
        modifier = modifier.clickable(onClickLabel = "Abrir registro", onClick = onClick),
        leadingContent = { RecordTypeBadge(item.type) },
        overlineContent = { Text(item.type.label) },
        headlineContent = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = item.detail?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        trailingContent = whenText?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTypePickerSheet(
    onDismiss: () -> Unit,
    onPick: (RecordType) -> Unit,
    subtitle: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "O que você quer registrar?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .semantics { heading() },
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            pickableRecordTypes.forEach { type ->
                ListItem(
                    modifier = Modifier.clickable { onPick(type) },
                    leadingContent = { RecordTypeBadge(type) },
                    headlineContent = { Text(type.label) },
                    supportingContent = { Text(type.hint) },
                )
            }
        }
    }
}
