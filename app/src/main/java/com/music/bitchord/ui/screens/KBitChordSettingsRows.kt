package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * KBitChord (K-10): a whole-number slider drawn as a sub-row — no icon, no
 * divider, pulled up under the setting it qualifies, like [SettingsSubRow].
 * Kept here rather than in SettingsSheet.kt so upstream edits there never
 * conflict with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KBitChordSubSlider(
    title: String,
    subtitle: String,
    value: String,
    current: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    enabled: Boolean,
) {
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outline,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .padding(start = ROW_INSET, end = ROW_INSET, top = 0.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = current.toFloat(),
            onValueChange = { onValue(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            enabled = enabled,
            colors = colors,
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    enabled = enabled,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
        )
    }
}
