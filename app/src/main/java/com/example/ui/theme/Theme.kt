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
    val contrastPillBg: Color,
    val contrastPillText: Color,
    val hairlineBorder: Color,
    val positiveGreen: Color,
    val positiveGreenContainer: Color,
    val onPositiveGreenContainer: Color,
    val expenseCoral: Color,
    val cobaltAccent: Color,
    val cobaltContainer: Color,
    val eWalletTint: Color
)

private val LightExtraColors = LedgerExtraColors(
    contrastPillBg = LedgerInkPillBg,
    contrastPillText = LedgerInkPillText,
    hairlineBorder = LedgerRuleLight,
    positiveGreen = LedgerEmeraldPositive,
    positiveGreenContainer = LedgerPineContainer,
    onPositiveGreenContainer = OnLedgerPineContainer,
    expenseCoral = LedgerTerracotta,
    cobaltAccent = LedgerCobaltSecondary,
    cobaltContainer = LedgerCobaltContainer,
    eWalletTint = LedgerCobaltSecondary
)

private val DarkExtraColors = LedgerExtraColors(
    contrastPillBg = Color(0xFF232825),
    contrastPillText = LedgerInkDark,
    hairlineBorder = LedgerRuleDark,
    positiveGreen = LedgerSageMintDark,
    positiveGreenContainer = LedgerPineDarkContainer,
    onPositiveGreenContainer = OnLedgerPineDarkContainer,
    expenseCoral = LedgerTerracottaDark,
    cobaltAccent = LedgerCobaltDark,
    cobaltContainer = LedgerCobaltDarkContainer,
    eWalletTint = LedgerCobaltDark
)

val LocalLedgerExtraColors = staticCompositionLocalOf { LightExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = LedgerPinePrimary,
    onPrimary = Color.White,
    primaryContainer = LedgerPineContainer,
    onPrimaryContainer = OnLedgerPineContainer,
    secondary = LedgerCobaltSecondary,
    onSecondary = Color.White,
    secondaryContainer = LedgerCobaltContainer,
    onSecondaryContainer = OnLedgerCobaltContainer,
    tertiary = LedgerTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = LedgerTerracottaContainer,
    onTertiaryContainer = OnLedgerTerracottaContainer,
    error = LedgerTerracotta,
    onError = Color.White,
    errorContainer = LedgerTerracottaContainer,
    onErrorContainer = OnLedgerTerracottaContainer,
    background = LedgerPaperLight,
    onBackground = LedgerInkPrimary,
    surface = LedgerSurfaceWhite,
    onSurface = LedgerInkPrimary,
    surfaceVariant = LedgerStoneLight,
    onSurfaceVariant = LedgerInkSecondary,
    surfaceContainerLowest = LedgerSurfaceWhite,
    surfaceContainerLow = Color(0xFFFAF8F4),
    surfaceContainer = LedgerStoneLight,
    surfaceContainerHigh = LedgerTrackLight,
    surfaceContainerHighest = Color(0xFFD8D2C5),
    outline = LedgerOutlineLight,
    outlineVariant = LedgerRuleLight
)

private val SageDarkColorScheme = darkColorScheme(
    primary = LedgerSageMintDark,
    onPrimary = Color(0xFF042619),
    primaryContainer = LedgerPineDarkContainer,
    onPrimaryContainer = OnLedgerPineDarkContainer,
    secondary = LedgerCobaltDark,
    onSecondary = Color(0xFF061E4A),
    secondaryContainer = LedgerCobaltDarkContainer,
    onSecondaryContainer = OnLedgerCobaltDarkContainer,
    tertiary = LedgerTerracottaDark,
    onTertiary = Color(0xFF3E1208),
    tertiaryContainer = LedgerTerracottaDarkContainer,
    onTertiaryContainer = OnLedgerTerracottaDarkContainer,
    error = LedgerTerracottaDark,
    onError = Color(0xFF3E1208),
    errorContainer = LedgerTerracottaDarkContainer,
    onErrorContainer = OnLedgerTerracottaDarkContainer,
    background = LedgerPaperDark,
    onBackground = LedgerInkDark,
    surface = LedgerSurfaceDark,
    onSurface = LedgerInkDark,
    surfaceVariant = LedgerStoneDark,
    onSurfaceVariant = LedgerInkMutedDark,
    surfaceContainerLowest = Color(0xFF090B0A),
    surfaceContainerLow = Color(0xFF131614),
    surfaceContainer = LedgerStoneDark,
    surfaceContainerHigh = LedgerTrackDark,
    surfaceContainerHighest = Color(0xFF363C38),
    outline = LedgerOutlineDark,
    outlineVariant = LedgerRuleDark
)

val SageShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
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
