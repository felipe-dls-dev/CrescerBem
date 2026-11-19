package br.com.crescerbem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Gráfico de linha simples (sem curvas de referência) para ver a evolução de uma medida.
 * O eixo X é proporcional ao tempo, então intervalos irregulares aparecem corretamente.
 */
@Composable
fun MiniLineChart(
    points: List<Pair<LocalDate, Double>>,
    color: Color,
    description: String,
    minLabel: String,
    maxLabel: String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    val sorted = points.sortedBy { it.first }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 6.dp)
                .semantics { contentDescription = description },
        ) {
            val firstDay = sorted.first().first
            val totalDays = ChronoUnit.DAYS.between(firstDay, sorted.last().first).coerceAtLeast(1)
            val minValue = sorted.minOf { it.second }
            val maxValue = sorted.maxOf { it.second }
            val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
            fun x(day: LocalDate) = size.width * ChronoUnit.DAYS.between(firstDay, day) / totalDays.toFloat()
            fun y(value: Double) = size.height - (size.height * ((value - minValue) / range)).toFloat()

            for (i in 0..3) {
                val gy = size.height * i / 3f
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            val path = Path()
            sorted.forEachIndexed { index, (day, value) ->
                val p = Offset(x(day), y(value))
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
            sorted.forEach { (day, value) -> drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(day), y(value))) }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
