package br.com.crescerbem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

/**
 * Tipografia com fonte do sistema e alturas de linha generosas para leitura rápida.
 * Os tamanhos em sp acompanham a escala de fonte definida no Android.
 */
val CrescerBemTypography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = Base.titleLarge.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Base.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = Base.titleSmall.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = Base.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = Base.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = Base.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = Base.labelLarge.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = Base.labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)
