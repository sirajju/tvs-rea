package com.sxplus.rider.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette — electric lime + deep carbon (sporty EV feel)
val Lime = Color(0xFFC6F432)
val LimeDark = Color(0xFF9FCB1E)
val Carbon = Color(0xFF0E1013)
val CarbonElevated = Color(0xFF1A1D22)
val CarbonCard = Color(0xFF212429)
val Ash = Color(0xFF9AA0A6)
val Danger = Color(0xFFFF5252)
val Success = Color(0xFF4CD080)
val Sky = Color(0xFF4FC3F7)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Carbon,
    primaryContainer = LimeDark,
    onPrimaryContainer = Carbon,
    secondary = Sky,
    onSecondary = Carbon,
    background = Carbon,
    onBackground = Color(0xFFF2F3F5),
    surface = CarbonElevated,
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = CarbonCard,
    onSurfaceVariant = Ash,
    error = Danger,
    outline = Color(0xFF34383E),
)

private val LightColors = lightColorScheme(
    primary = LimeDark,
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    error = Danger,
)

@Composable
fun SxpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colors.background.luminance() > 0.5f
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
