package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class LedgerExtraColors(
    val isDark: Boolean,
    val contrastPillBg: Color,
    val contrastPillText: Color,
    val hairlineBorder: Color,
    val bentoSheenBorder: Color,
    val glassDockBg: Color,
    val glassDockBorder: Color,
    val positiveGreen: Color,
    val positiveGreenContainer: Color,
    val onPositiveGreenContainer: Color,
    val expenseCoral: Color,
    val expenseCoralContainer: Color,
    val onExpenseCoralContainer: Color,
    val cobaltAccent: Color,
    val cobaltContainer: Color,
    val onCobaltContainer: Color,
    val eWalletTint: Color,
    val amberAccent: Color
)

private val LightExtraColors = LedgerExtraColors(
    isDark = false,
    contrastPillBg = DaylightInkPrimary,
    contrastPillText = DaylightSurfaceWhite,
    hairlineBorder = DaylightHairlineLight,
    bentoSheenBorder = Color(0xFFFFFFFF),
    glassDockBg = DaylightGlassDockLight,
    glassDockBorder = DaylightHairlineLight,
    positiveGreen = DaylightMintPrimary,
    positiveGreenContainer = DaylightMintContainer,
    onPositiveGreenContainer = DaylightOnMintContainer,
    expenseCoral = DaylightCoralTertiary,
    expenseCoralContainer = DaylightCoralContainer,
    onExpenseCoralContainer = DaylightOnCoralContainer,
    cobaltAccent = DaylightSkySecondary,
    cobaltContainer = DaylightSkyContainer,
    onCobaltContainer = DaylightOnSkyContainer,
    eWalletTint = DaylightSkySecondary,
    amberAccent = Color(0xFFD97706)
)

private val DarkExtraColors = LedgerExtraColors(
    isDark = true,
    contrastPillBg = BentoSlateHighest,
    contrastPillText = BentoInkPrimary,
    hairlineBorder = BentoHairlineDark,
    bentoSheenBorder = BentoSheenDark,
    glassDockBg = BentoGlassDockDark,
    glassDockBorder = Color(0x22FFFFFF),
    positiveGreen = BentoMintPrimary,
    positiveGreenContainer = BentoMintContainer,
    onPositiveGreenContainer = BentoOnMintContainer,
    expenseCoral = BentoCoralTertiary,
    expenseCoralContainer = BentoCoralContainer,
    onExpenseCoralContainer = BentoOnCoralContainer,
    cobaltAccent = BentoSkySecondary,
    cobaltContainer = BentoSkyContainer,
    onCobaltContainer = BentoOnSkyContainer,
    eWalletTint = BentoSkySecondary,
    amberAccent = Color(0xFFFBBF24)
)

val LocalLedgerExtraColors = staticCompositionLocalOf { DarkExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = DaylightMintPrimary,
    onPrimary = Color.White,
    primaryContainer = DaylightMintContainer,
    onPrimaryContainer = DaylightOnMintContainer,
    secondary = DaylightSkySecondary,
    onSecondary = Color.White,
    secondaryContainer = DaylightSkyContainer,
    onSecondaryContainer = DaylightOnSkyContainer,
    tertiary = DaylightCoralTertiary,
    onTertiary = Color.White,
    tertiaryContainer = DaylightCoralContainer,
    onTertiaryContainer = DaylightOnCoralContainer,
    error = DaylightCoralTertiary,
    onError = Color.White,
    errorContainer = DaylightCoralContainer,
    onErrorContainer = DaylightOnCoralContainer,
    background = DaylightCanvasBg,
    onBackground = DaylightInkPrimary,
    surface = DaylightSurfaceWhite,
    onSurface = DaylightInkPrimary,
    surfaceVariant = DaylightSlateWell,
    onSurfaceVariant = DaylightInkMuted,
    surfaceContainerLowest = DaylightSurfaceWhite,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = DaylightSlateWell,
    surfaceContainerHigh = DaylightTrackLight,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8),
    outlineVariant = DaylightHairlineLight
)

private val SageDarkColorScheme = darkColorScheme(
    primary = BentoMintPrimary,
    onPrimary = BentoOnMintPrimary,
    primaryContainer = BentoMintContainer,
    onPrimaryContainer = BentoOnMintContainer,
    secondary = BentoSkySecondary,
    onSecondary = BentoOnSkySecondary,
    secondaryContainer = BentoSkyContainer,
    onSecondaryContainer = BentoOnSkyContainer,
    tertiary = BentoCoralTertiary,
    onTertiary = BentoOnCoralTertiary,
    tertiaryContainer = BentoCoralContainer,
    onTertiaryContainer = BentoOnCoralContainer,
    error = BentoCoralTertiary,
    onError = BentoOnCoralTertiary,
    errorContainer = BentoCoralContainer,
    onErrorContainer = BentoOnCoralContainer,
    background = BentoObsidianBg,
    onBackground = BentoInkPrimary,
    surface = BentoSlateSurface,
    onSurface = BentoInkPrimary,
    surfaceVariant = BentoSlateElevated,
    onSurfaceVariant = BentoInkMuted,
    surfaceContainerLowest = Color(0xFF050609),
    surfaceContainerLow = BentoSlateElevated,
    surfaceContainer = BentoSlateElevated,
    surfaceContainerHigh = BentoSlateTrack,
    surfaceContainerHighest = BentoSlateHighest,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF23293A)
)

val SageShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SageLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SageDarkColorScheme else SageLightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors
    CompositionLocalProvider(LocalLedgerExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SageTypography,
            shapes = SageShapes,
            content = content
        )
    }
}
