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
    contrastPillBg = EditorialObsidianPill,
    contrastPillText = EditorialObsidianText,
    hairlineBorder = EditorialBorderLight,
    positiveGreen = EditorialPositiveGreen,
    positiveGreenContainer = EditorialSageContainer,
    onPositiveGreenContainer = OnEditorialSageContainer,
    expenseCoral = EditorialCoralError,
    cobaltAccent = EditorialCobaltSecondary,
    cobaltContainer = EditorialCobaltContainer,
    eWalletTint = Color(0xFF0A66FF)
)

private val DarkExtraColors = LedgerExtraColors(
    contrastPillBg = Color(0xFF232834),
    contrastPillText = EditorialInkDark,
    hairlineBorder = EditorialBorderDark,
    positiveGreen = EditorialMintDark,
    positiveGreenContainer = EditorialSageDarkContainer,
    onPositiveGreenContainer = OnEditorialSageDarkContainer,
    expenseCoral = EditorialCoralDark,
    cobaltAccent = EditorialCobaltDark,
    cobaltContainer = EditorialCobaltDarkContainer,
    eWalletTint = EditorialCobaltDark
)

val LocalLedgerExtraColors = staticCompositionLocalOf { LightExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = EditorialSagePrimary,
    onPrimary = Color.White,
    primaryContainer = EditorialSageContainer,
    onPrimaryContainer = OnEditorialSageContainer,
    secondary = EditorialCobaltSecondary,
    onSecondary = Color.White,
    secondaryContainer = EditorialCobaltContainer,
    onSecondaryContainer = OnEditorialCobaltContainer,
    tertiary = EditorialAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EditorialAmberContainer,
    onTertiaryContainer = OnEditorialAmberContainer,
    error = EditorialCoralError,
    onError = Color.White,
    errorContainer = EditorialCoralContainer,
    onErrorContainer = OnEditorialCoralContainer,
    background = EditorialCanvasLight,
    onBackground = EditorialInkPrimary,
    surface = EditorialCardWhite,
    onSurface = EditorialInkPrimary,
    surfaceVariant = EditorialWellLight,
    onSurfaceVariant = EditorialInkSecondary,
    surfaceContainerLowest = EditorialCardWhite,
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainer = EditorialWellLight,
    surfaceContainerHigh = EditorialTrackLight,
    surfaceContainerHighest = Color(0xFFD6DAE4),
    outline = EditorialOutlineLight,
    outlineVariant = EditorialBorderLight
)

private val SageDarkColorScheme = darkColorScheme(
    primary = EditorialMintDark,
    onPrimary = Color(0xFF03291B),
    primaryContainer = EditorialSageDarkContainer,
    onPrimaryContainer = OnEditorialSageDarkContainer,
    secondary = EditorialCobaltDark,
    onSecondary = Color(0xFF041E49),
    secondaryContainer = EditorialCobaltDarkContainer,
    onSecondaryContainer = OnEditorialCobaltDarkContainer,
    tertiary = EditorialAmberDark,
    onTertiary = Color(0xFF391702),
    tertiaryContainer = EditorialAmberDarkContainer,
    onTertiaryContainer = OnEditorialAmberDarkContainer,
    error = EditorialCoralDark,
    onError = Color(0xFF470615),
    errorContainer = EditorialCoralDarkContainer,
    onErrorContainer = OnEditorialCoralDarkContainer,
    background = EditorialCanvasDark,
    onBackground = EditorialInkDark,
    surface = EditorialCardDark,
    onSurface = EditorialInkDark,
    surfaceVariant = EditorialWellDark,
    onSurfaceVariant = EditorialInkMutedDark,
    surfaceContainerLowest = Color(0xFF07080A),
    surfaceContainerLow = Color(0xFF101217),
    surfaceContainer = EditorialWellDark,
    surfaceContainerHigh = EditorialTrackDark,
    surfaceContainerHighest = Color(0xFF343A48),
    outline = EditorialOutlineDark,
    outlineVariant = EditorialBorderDark
)

val SageShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
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
