package com.sxplus.rider.ui.theme

import androidx.compose.ui.graphics.Color
import com.sxplus.rider.data.local.SettingsStore.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Accent presets selectable per vehicle. */
val ACCENTS: List<Color> = listOf(
    Lime,               // 0
    Sky,                // 1
    Color(0xFFFF9800),  // 2 amber
    Color(0xFFBB86FC),  // 3 violet
    Color(0xFFFF5252),  // 4 red
    Color(0xFF4CD080),  // 5 green
)

/** Live theme state (mode + accent) applied at the app root. */
@Singleton
class ThemeController @Inject constructor() {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode = _mode.asStateFlow()

    private val _accent = MutableStateFlow(0)
    val accent = _accent.asStateFlow()

    fun setMode(m: ThemeMode) { _mode.value = m }
    fun setAccent(index: Int) { _accent.value = index.coerceIn(0, ACCENTS.lastIndex) }
    fun accentColor(): Color = ACCENTS.getOrElse(_accent.value) { Lime }
}
