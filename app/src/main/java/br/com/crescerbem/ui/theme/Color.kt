package br.com.crescerbem.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base do Crescer Bem: verde-sálvia (calma), pêssego (acolhimento) e lavanda (apoio).
// Responsável pelo sistema visual: Codex (tarefa 17). Mantenha os nomes públicos estáveis.

internal val Sage10 = Color(0xFF00201D)
internal val Sage30 = Color(0xFF1E504C)
internal val Sage40 = Color(0xFF2F6F6A)
internal val Sage80 = Color(0xFF96D3CB)
internal val Sage90 = Color(0xFFCFEDE8)

internal val Peach10 = Color(0xFF3A0A00)
internal val Peach30 = Color(0xFF7A3626)
internal val Peach40 = Color(0xFF9A4A38)
internal val Peach80 = Color(0xFFFFB4A2)
internal val Peach90 = Color(0xFFFFDAD1)

internal val Lavender10 = Color(0xFF1F1147)
internal val Lavender30 = Color(0xFF4B3D80)
internal val Lavender40 = Color(0xFF63559A)
internal val Lavender80 = Color(0xFFCBBEFF)
internal val Lavender90 = Color(0xFFE7DEFF)

internal val Cream98 = Color(0xFFFAF8F4)
internal val Cream95 = Color(0xFFF1EEE8)
internal val Ink10 = Color(0xFF1B1C1A)
internal val Ink30 = Color(0xFF45474A)
internal val Ink80 = Color(0xFFC6C7C3)
internal val Ink90 = Color(0xFFE3E3DF)
internal val Night6 = Color(0xFF121413)
internal val Night12 = Color(0xFF1E201F)

// Superfícies dos blocos (cartões, seções, barra inferior). No claro, sálvia acinzentado;
// no escuro, cinza-esverdeado. Os números indicam a luminosidade aproximada (tom).
internal val SageGray92 = Color(0xFFE8EEEB)
internal val SageGray91 = Color(0xFFE5ECE9)
internal val SageGray89 = Color(0xFFDFE7E3)
internal val SageGray87 = Color(0xFFD8E1DD)
internal val SageGray85 = Color(0xFFD2DBD7)
internal val SageGray78 = Color(0xFFBFCAC5)
internal val Night4 = Color(0xFF0C0E0D)
internal val Night14 = Color(0xFF1F2423)
internal val Night16 = Color(0xFF242A28)
internal val Night19 = Color(0xFF2C3230)
internal val Night23 = Color(0xFF363C3A)

internal val Error40 = Color(0xFFB3261E)
internal val Error80 = Color(0xFFF2B8B5)
internal val Error90 = Color(0xFFF9DEDC)
internal val Error10 = Color(0xFF410E0B)

/**
 * Cores por categoria de registro (marcadores do calendário, ícones e chips).
 *
 * Elas mantêm ao menos 3:1 de contraste tanto sobre [Cream98] quanto sobre
 * [Night6]. Sempre acompanhe a cor de um ícone, rótulo ou padrão; ela não é a
 * única forma de comunicar a categoria.
 */
object CategoryColors {
    val moment = Color(0xFFC2185B)
    val vaccine = Color(0xFF2E7D32)
    val appointment = Color(0xFF1565C0)
    val health = Color(0xFFB94600)
    val growth = Color(0xFF9333EA)
    val feeding = Color(0xFF00796B)
    val medication = Color(0xFF3949AB)
}

/**
 * Cores de status (vacinas, alertas).
 *
 * Use com texto e ícone de apoio (por exemplo, “Aplicada”), nunca só pela cor.
 */
object StatusColors {
    val done = Color(0xFF2E7D32)
    val pending = Color(0xFF9A5A00)
    val overdue = Color(0xFFC62828)
    val neutral = Color(0xFF626563)
}

// ---------------------------------------------------------------------------
// Paleta de cores por bebê — 8 tons pastéis curados, contraste validado.
// Apenas primary e primaryContainer variam; toda a paleta de superfícies e
// erros permanece inalterada. Índice 0 é o verde-menta padrão do app.
// ---------------------------------------------------------------------------

/** Entrada da paleta de cor por bebê, com variantes para tema claro e escuro. */
data class BabyPaletteEntry(
    val label: String,
    // Tema claro
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Tema escuro
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
)

/** Lista ordenada de paletas disponíveis. O índice é armazenado em [br.com.crescerbem.data.model.Baby.colorIndex]. */
val BabyPalette: List<BabyPaletteEntry> = listOf(
    // 0 — Verde-menta (padrão atual do app)
    BabyPaletteEntry(
        label = "Verde-menta",
        primary = Sage40, onPrimary = Color.White,
        primaryContainer = Sage90, onPrimaryContainer = Sage10,
        primaryDark = Sage80, onPrimaryDark = Sage10,
        primaryContainerDark = Sage30, onPrimaryContainerDark = Sage90,
    ),
    // 1 — Azul-bebê
    BabyPaletteEntry(
        label = "Azul",
        primary = Color(0xFF2A5FA5), onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001B3D),
        primaryDark = Color(0xFF9AC4F8), onPrimaryDark = Color(0xFF003062),
        primaryContainerDark = Color(0xFF08458A), onPrimaryContainerDark = Color(0xFFD3E4FF),
    ),
    // 2 — Lilás
    BabyPaletteEntry(
        label = "Lilás",
        primary = Lavender40, onPrimary = Color.White,
        primaryContainer = Lavender90, onPrimaryContainer = Lavender10,
        primaryDark = Lavender80, onPrimaryDark = Lavender10,
        primaryContainerDark = Lavender30, onPrimaryContainerDark = Lavender90,
    ),
    // 3 — Rosa-antigo
    BabyPaletteEntry(
        label = "Rosa",
        primary = Color(0xFF9C4A6B), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD8E6), onPrimaryContainer = Color(0xFF3D0024),
        primaryDark = Color(0xFFF4B8D0), onPrimaryDark = Color(0xFF5C1133),
        primaryContainerDark = Color(0xFF7B2F51), onPrimaryContainerDark = Color(0xFFFFD8E6),
    ),
    // 4 — Amarelo-mostarda
    BabyPaletteEntry(
        label = "Amarelo",
        primary = Color(0xFF7A6000), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE07A), onPrimaryContainer = Color(0xFF261A00),
        primaryDark = Color(0xFFEDD56A), onPrimaryDark = Color(0xFF3D2F00),
        primaryContainerDark = Color(0xFF5C4700), onPrimaryContainerDark = Color(0xFFFFE07A),
    ),
    // 5 — Pêssego
    BabyPaletteEntry(
        label = "Pêssego",
        primary = Peach40, onPrimary = Color.White,
        primaryContainer = Peach90, onPrimaryContainer = Peach10,
        primaryDark = Peach80, onPrimaryDark = Peach10,
        primaryContainerDark = Peach30, onPrimaryContainerDark = Peach90,
    ),
    // 6 — Turquesa
    BabyPaletteEntry(
        label = "Turquesa",
        primary = Color(0xFF1A6E7A), onPrimary = Color.White,
        primaryContainer = Color(0xFFB3EEFF), onPrimaryContainer = Color(0xFF001F26),
        primaryDark = Color(0xFF8AD4DF), onPrimaryDark = Color(0xFF003640),
        primaryContainerDark = Color(0xFF00525C), onPrimaryContainerDark = Color(0xFFB3EEFF),
    ),
    // 7 — Areia
    BabyPaletteEntry(
        label = "Areia",
        primary = Color(0xFF7A5C3A), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDDBB), onPrimaryContainer = Color(0xFF2C1600),
        primaryDark = Color(0xFFE0C8A8), onPrimaryDark = Color(0xFF442B10),
        primaryContainerDark = Color(0xFF5E4124), onPrimaryContainerDark = Color(0xFFFFDDBB),
    ),
)
