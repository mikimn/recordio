package com.mikimn.recordio.layout

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikimn.compose.material.icons.Pause
import com.mikimn.recordio.timerFormatted
import java.time.Duration
import kotlin.math.max
import kotlin.math.min


@Composable
fun AudioPlayer(
    duration: Duration,
    onPlayStatus: ((isPlaying: Boolean) -> Unit) = {},
    onPlaceSelected: ((place: Int) -> Unit) = {}
) {
    val durationSeconds = duration.seconds
    var sliderValue by remember { mutableStateOf((0.3f * durationSeconds).toInt()) }
    var isPlaying by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                valueRange = 0f..(durationSeconds.toFloat()),
                value = sliderValue.toFloat(),
                // steps = durationSeconds.toInt() - 1, // Excluding 0
                onValueChange = {
                    sliderValue = it.toInt()
                    onPlaceSelected(sliderValue)
                })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = Duration.ofSeconds(sliderValue.toLong()).timerFormatted())
            Text(
                text = "-${
                    duration.minusSeconds(sliderValue.toLong()).timerFormatted()
                }"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                sliderValue = max(sliderValue - 10, 0)
                onPlaceSelected(sliderValue)
            }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = {
                isPlaying = !isPlaying
                onPlayStatus(isPlaying)
            }) {
                Crossfade(
                    targetState = isPlaying,
                ) {
                    Icon(
                        if (it) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = {
                sliderValue = min(sliderValue + 10, durationSeconds.toInt())
                onPlaceSelected(sliderValue)
            }) {
                Icon(Icons.Default.FastForward, contentDescription = "Forward")
            }
        }
    }
}