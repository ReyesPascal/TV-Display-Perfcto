package com.reyespascal.picturehelper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Space kept clear from every screen edge (tvOS safe area: 80x60 pt on 1920x1080 = 40x30 dp here). */
val SafeH = 48.dp
val SafeV = 30.dp

private val Colors = darkColorScheme(
    primary = Color(0xFFFFD860),
    onPrimary = Color(0xFF111111),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0E1014),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFFC8CCD4),
)

@Composable
fun PictureHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
