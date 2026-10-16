package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SMCutColorScheme = darkColorScheme(
  primary = CoralRed,
  onPrimary = TextInk,
  primaryContainer = CoralRedDark,
  onPrimaryContainer = TextInk,
  secondary = MintCyan,
  onSecondary = DarkBg,
  secondaryContainer = BorderLine,
  onSecondaryContainer = MintCyan,
  tertiary = VioletPurple,
  onTertiary = TextInk,
  background = DarkBg,
  onBackground = TextInk,
  surface = PanelBg,
  onSurface = TextInk,
  surfaceVariant = PanelElevated,
  onSurfaceVariant = TextMuted,
  outline = BorderLine,
  outlineVariant = BorderLine
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = DarkBg.toArgb()
        window.navigationBarColor = DarkBg.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
      }
    }
  }

  MaterialTheme(colorScheme = SMCutColorScheme, typography = Typography, content = content)
}
