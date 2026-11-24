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
    contrastPillBg = SumDifLightInkPrimary,
    contrastPillText = SumDifLightSurface,
    hairlineBorder = SumDifLightHairline,
    bentoSheenBorder = Color(0xFFFFFFFF),
    glassDockBg = SumDifLightGlassDock,
    glassDockBorder = SumDifLightHairline,
    positiveGreen = SumDifMintPrimary,
    positiveGreenContainer = SumDifMintContainer,
    onPositiveGreenContainer = SumDifOnMintContainer,
    expenseCoral = SumDifCoralTertiary,
    expenseCoralContainer = SumDifCoralContainer,
    onExpenseCoralContainer = SumDifOnCoralContainer,
    cobaltAccent = SumDifSkySecondary,
    cobaltContainer = SumDifSkyContainer,
    onCobaltContainer = SumDifOnSkyContainer,
    eWalletTint = SumDifSkySecondary,
    amberAccent = Color(0xFFD97706)
)

private val DarkExtraColors = LedgerExtraColors(
    isDark = true,
    contrastPillBg = SumDifDarkHighest,
    contrastPillText = SumDifDarkInkPrimary,
    hairlineBorder = SumDifDarkHairline,
    bentoSheenBorder = Color(0x24FFFFFF),
    glassDockBg = SumDifDarkGlassDock,
    glassDockBorder = Color(0x22FFFFFF),
    positiveGreen = SumDifDarkMintPrimary,
    positiveGreenContainer = SumDifDarkMintContainer,
    onPositiveGreenContainer = SumDifDarkOnMintContainer,
    expenseCoral = SumDifDarkCoralTertiary,
    expenseCoralContainer = SumDifDarkCoralContainer,
    onExpenseCoralContainer = SumDifDarkOnCoralContainer,
    cobaltAccent = SumDifDarkSkySecondary,
    cobaltContainer = SumDifDarkSkyContainer,
    onCobaltContainer = SumDifDarkOnSkyContainer,
    eWalletTint = SumDifDarkSkySecondary,
    amberAccent = Color(0xFFFBBF24)
)

val LocalLedgerExtraColors = staticCompositionLocalOf { LightExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = SumDifMintPrimary,
    onPrimary = Color.White,
    primaryContainer = SumDifMintContainer,
    onPrimaryContainer = SumDifOnMintContainer,
    secondary = SumDifSkySecondary,
    onSecondary = Color.White,
    secondaryContainer = SumDifSkyContainer,
    onSecondaryContainer = SumDifOnSkyContainer,
    tertiary = SumDifCoralTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SumDifCoralContainer,
    onTertiaryContainer = SumDifOnCoralContainer,
    error = SumDifCoralTertiary,
    onError = Color.White,
    errorContainer = SumDifCoralContainer,
    onErrorContainer = SumDifOnCoralContainer,
    background = SumDifLightCanvas,
    onBackground = SumDifLightInkPrimary,
    surface = SumDifLightSurface,
    onSurface = SumDifLightInkPrimary,
    surfaceVariant = SumDifLightSurfaceVariant,
    onSurfaceVariant = SumDifLightInkMuted,
    surfaceContainerLowest = SumDifLightSurface,
    surfaceContainerLow = Color(0xFFF2F8F5),
    surfaceContainer = SumDifLightSurfaceVariant,
    surfaceContainerHigh = SumDifLightTrack,
    surfaceContainerHighest = Color(0xFFB8D4C7),
    outline = Color(0xFF94A3B8),
    outlineVariant = SumDifLightHairline
)

private val SageDarkColorScheme = darkColorScheme(
    primary = SumDifDarkMintPrimary,
    onPrimary = Color(0xFF042217),
    primaryContainer = SumDifDarkMintContainer,
    onPrimaryContainer = SumDifDarkOnMintContainer,
    secondary = SumDifDarkSkySecondary,
    onSecondary = Color(0xFF052033),
    secondaryContainer = SumDifDarkSkyContainer,
    onSecondaryContainer = SumDifDarkOnSkyContainer,
    tertiary = SumDifDarkCoralTertiary,
    onTertiary = Color(0xFF3A0712),
    tertiaryContainer = SumDifDarkCoralContainer,
    onTertiaryContainer = SumDifDarkOnCoralContainer,
    error = SumDifDarkCoralTertiary,
    onError = Color(0xFF3A0712),
    errorContainer = SumDifDarkCoralContainer,
    onErrorContainer = SumDifDarkOnCoralContainer,
    background = SumDifDarkCanvas,
    onBackground = SumDifDarkInkPrimary,
    surface = SumDifDarkSurface,
    onSurface = SumDifDarkInkPrimary,
    surfaceVariant = SumDifDarkSurfaceVariant,
    onSurfaceVariant = SumDifDarkInkMuted,
    surfaceContainerLowest = Color(0xFF080D0B),
    surfaceContainerLow = SumDifDarkSurfaceVariant,
    surfaceContainer = SumDifDarkSurfaceVariant,
    surfaceContainerHigh = SumDifDarkTrack,
    surfaceContainerHighest = SumDifDarkHighest,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF263C34)
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
