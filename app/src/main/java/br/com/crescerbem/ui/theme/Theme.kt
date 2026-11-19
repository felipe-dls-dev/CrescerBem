package br.com.crescerbem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Sage40,
    onPrimary = Color.White,
    primaryContainer = Sage90,
    onPrimaryContainer = Sage10,
    secondary = Peach40,
    onSecondary = Color.White,
    secondaryContainer = Peach90,
    onSecondaryContainer = Peach10,
    tertiary = Lavender40,
    onTertiary = Color.White,
    tertiaryContainer = Lavender90,
    onTertiaryContainer = Lavender10,
    background = Cream98,
    onBackground = Ink10,
    surface = Cream98,
    onSurface = Ink10,
    surfaceVariant = SageGray92,
    onSurfaceVariant = Ink30,
    // Blocos em sálvia acinzentado, claramente distintos do fundo creme.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SageGray91,
    surfaceContainer = SageGray89,
    surfaceContainerHigh = SageGray87,
    surfaceContainerHighest = SageGray85,
    // Borda visível sem competir com os dados de saúde na tela.
    outline = Color(0xFF6F7274),
    outlineVariant = SageGray78,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
)

private val DarkColors = darkColorScheme(
    primary = Sage80,
    onPrimary = Sage10,
    primaryContainer = Sage30,
    onPrimaryContainer = Sage90,
    secondary = Peach80,
    onSecondary = Peach10,
    secondaryContainer = Peach30,
    onSecondaryContainer = Peach90,
    tertiary = Lavender80,
    onTertiary = Lavender10,
    tertiaryContainer = Lavender30,
    onTertiaryContainer = Lavender90,
    background = Night6,
    onBackground = Ink90,
    surface = Night6,
    onSurface = Ink90,
    surfaceVariant = Night16,
    onSurfaceVariant = Ink80,
    surfaceContainerLowest = Night4,
    surfaceContainerLow = Night14,
    surfaceContainer = Night16,
    surfaceContainerHigh = Night19,
    surfaceContainerHighest = Night23,
    outline = Color(0xFF969997),
    outlineVariant = Color(0xFF3F4644),
    error = Error80,
    onError = Error10,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Error90,
)

val CrescerBemShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun CrescerBemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorIndex: Int = 0,
    content: @Composable () -> Unit,
) {
    val palette = BabyPalette.getOrElse(colorIndex) { BabyPalette[0] }
    // Índice 0 (verde-menta) mantém as superfícies originais; as demais cores tingem
    // cartões, menus e barras com o tom do bebê, sem mexer em texto, fundo e erros.
    val tinted = colorIndex != 0 && BabyPalette.indices.contains(colorIndex)
    val colorScheme = if (darkTheme) {
        val base = DarkColors.copy(
            primary = palette.primaryDark,
            onPrimary = palette.onPrimaryDark,
            primaryContainer = palette.primaryContainerDark,
            onPrimaryContainer = palette.onPrimaryContainerDark,
            // Botões tonais, chips e barra inferior acompanham a cor do bebê.
            secondary = palette.primaryDark,
            onSecondary = palette.onPrimaryDark,
            secondaryContainer = palette.primaryContainerDark,
            onSecondaryContainer = palette.onPrimaryContainerDark,
            // "Terciária" é a cor de aviso (pêssego/avermelhada), igual para todos os bebês.
            tertiary = Peach80,
            onTertiary = Peach10,
            tertiaryContainer = Peach30,
            onTertiaryContainer = Peach90,
        )
        if (!tinted) base else {
            val tone = palette.primaryContainerDark
            base.copy(
                surfaceVariant = lerp(Night6, tone, 0.30f),
                surfaceContainerLowest = Night4,
                surfaceContainerLow = lerp(Night6, tone, 0.22f),
                surfaceContainer = lerp(Night6, tone, 0.30f),
                surfaceContainerHigh = lerp(Night6, tone, 0.38f),
                surfaceContainerHighest = lerp(Night6, tone, 0.46f),
                outlineVariant = lerp(Night6, palette.primaryDark, 0.30f),
            )
        }
    } else {
        val base = LightColors.copy(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.primary,
            onSecondary = palette.onPrimary,
            secondaryContainer = palette.primaryContainer,
            onSecondaryContainer = palette.onPrimaryContainer,
            tertiary = Peach40,
            onTertiary = Color.White,
            tertiaryContainer = Peach90,
            onTertiaryContainer = Peach10,
        )
        if (!tinted) base else {
            val tone = palette.primaryContainer
            base.copy(
                surfaceVariant = lerp(Cream98, tone, 0.40f),
                surfaceContainerLow = lerp(Cream98, tone, 0.32f),
                surfaceContainer = lerp(Cream98, tone, 0.45f),
                surfaceContainerHigh = lerp(Cream98, tone, 0.58f),
                surfaceContainerHighest = lerp(Cream98, tone, 0.70f),
                outlineVariant = lerp(Cream98, palette.primary, 0.35f),
            )
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CrescerBemTypography,
        shapes = CrescerBemShapes,
        content = content,
    )
}
