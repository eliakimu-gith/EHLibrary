package com.eli.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkGreenColorScheme = darkColorScheme(
    primary = ForestPrimary,
    secondary = ForestSecondary,
    background = DarkGreenBackground,
    surface = DarkGreenSurface,
    surfaceVariant = DarkGreenSurfaceVariant,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted,
)

@Composable
fun VerdeReadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkGreenColorScheme,
        content = content
    )
}

fun Modifier.glassCard(
    cornerRadius: Int = 16
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(DarkGreenSurface.copy(alpha = 0.85f))
    .border(1.dp, GlassCardBorder, RoundedCornerShape(cornerRadius.dp))
