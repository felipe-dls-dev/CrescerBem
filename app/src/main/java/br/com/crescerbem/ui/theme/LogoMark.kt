package br.com.crescerbem.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Mesmo desenho do ícone do app (coração com broto), recortado sem a margem do ícone adaptativo.
private const val HEART =
    "M22,42 C22,42 0,28 0,13 C0,5.5 5.5,0 12.5,0 C16.6,0 20.1,2 22,5.2 " +
        "C23.9,2 27.4,0 31.5,0 C38.5,0 44,5.5 44,13 C44,28 22,42 22,42 Z"
private const val SPROUT =
    "M21,30 L21,16 C21,11 17,8 13,8 C13,13 16.5,16.5 21,16.8 Z " +
        "M23,22 L23,14 C23,10 26.5,7 31,7 C31,11.5 27.5,14.8 23,15 Z " +
        "M21,16 L23,16 L23,30 L21,30 Z"

/** Proporção largura/altura da logo. */
const val LOGO_ASPECT_RATIO = 44f / 42f

/** Logo do Crescer Bem com as cores do tema: coração em [heart] e broto em [sprout]. */
@Composable
fun rememberLogoMark(heart: Color, sprout: Color): ImageVector = remember(heart, sprout) {
    ImageVector.Builder(
        name = "CrescerBemLogo",
        defaultWidth = 44.dp,
        defaultHeight = 42.dp,
        viewportWidth = 44f,
        viewportHeight = 42f,
    )
        .addPath(pathData = addPathNodes(HEART), fill = SolidColor(heart))
        .addPath(pathData = addPathNodes(SPROUT), fill = SolidColor(sprout))
        .build()
}
