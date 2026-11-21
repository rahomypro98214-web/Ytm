package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SanaSemanticColors(
    val cardBorder: Color,
    val chatCanvas: Color,
    val heroBackground: Color,
    val heroOnBackground: Color,
    val heroSubtext: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val tealAccent: Color,
    val tealDeep: Color,
    val mintGlow: Color,
    val amberAccent: Color,
    val amberDeep: Color,
    val tealTint: Color,
    val amberTint: Color,
    val indigoTint: Color,
    val errorSurface: Color,
    val errorText: Color,
    val codeBackground: Color,
    val codeText: Color
)

private val LightExtras = SanaSemanticColors(
    cardBorder = SageLine,
    chatCanvas = SagePaperSoft,
    heroBackground = DeepIndigo,
    heroOnBackground = HeroCreamText,
    heroSubtext = IndigoLightText,
    userBubble = DeepIndigo,
    onUserBubble = HeroCreamText,
    tealAccent = LuminousTeal,
    tealDeep = TealDeep,
    mintGlow = MintGlow,
    amberAccent = WarmAmber,
    amberDeep = AmberDeep,
    tealTint = TealTint,
    amberTint = AmberTint,
    indigoTint = IndigoTint,
    errorSurface = ErrorTint,
    errorText = ErrorBrick,
    codeBackground = Color(0xFF10162B),
    codeText = Color(0xFFE8EEFC)
)

private val DarkExtras = SanaSemanticColors(
    cardBorder = DarkLine,
    chatCanvas = DarkBg,
    heroBackground = DeepIndigo,
    heroOnBackground = HeroCreamText,
    heroSubtext = IndigoLightText,
    userBubble = TealDeep,
    onUserBubble = PureWhite,
    tealAccent = MintGlow,
    tealDeep = LuminousTeal,
    mintGlow = MintGlow,
    amberAccent = WarmAmber,
    amberDeep = WarmAmber,
    tealTint = Color(0xFF123635),
    amberTint = Color(0xFF382A16),
    indigoTint = Color(0xFF232A4D),
    errorSurface = Color(0xFF3B1D1B),
    errorText = Color(0xFFF4A89C),
    codeBackground = Color(0xFF0A0D1C),
    codeText = Color(0xFFE8EEFC)
)

val LocalSanaColors = staticCompositionLocalOf { LightExtras }

private val LightColorScheme = lightColorScheme(
    primary = LuminousTeal,
    onPrimary = PureWhite,
    primaryContainer = TealTint,
    onPrimaryContainer = TealDeep,
    secondary = DeepIndigo,
    onSecondary = HeroCreamText,
    secondaryContainer = IndigoTint,
    onSecondaryContainer = DeepIndigo,
    tertiary = WarmAmber,
    onTertiary = DeepIndigo,
    tertiaryContainer = AmberTint,
    onTertiaryContainer = AmberDeep,
    background = SagePaper,
    onBackground = DeepIndigo,
    surface = PureWhite,
    onSurface = DeepIndigo,
    surfaceVariant = SageMuted,
    onSurfaceVariant = IndigoSoft,
    outline = SageLine,
    error = ErrorBrick,
    onError = PureWhite,
    errorContainer = ErrorTint,
    onErrorContainer = ErrorBrick
)

private val DarkColorScheme = darkColorScheme(
    primary = MintGlow,
    onPrimary = DeepIndigoDark,
    primaryContainer = Color(0xFF123635),
    onPrimaryContainer = MintGlow,
    secondary = IndigoLightText,
    onSecondary = DeepIndigoDark,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = WarmAmber,
    onTertiary = DeepIndigoDark,
    tertiaryContainer = Color(0xFF382A16),
    onTertiaryContainer = WarmAmber,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkLine,
    error = Color(0xFFF4A89C),
    onError = Color(0xFF47120C),
    errorContainer = Color(0xFF3B1D1B),
    onErrorContainer = Color(0xFFF4A89C)
)

val SanaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) DarkExtras else LightExtras

    CompositionLocalProvider(LocalSanaColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SanaTypography,
            shapes = SanaShapes,
            content = content
        )
    }
}
