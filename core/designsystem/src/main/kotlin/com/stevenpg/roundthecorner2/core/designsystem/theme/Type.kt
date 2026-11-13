package com.stevenpg.roundthecorner2.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/*
 * Type scale. Uses the platform sans-serif for now; a bundled brand font can be
 * dropped in here during the UI review milestone without touching any screen.
 * Headings get a heavier weight and tighter tracking for a more modern look.
 */
private val Base = Typography()

internal val RtcTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.Medium),
)
