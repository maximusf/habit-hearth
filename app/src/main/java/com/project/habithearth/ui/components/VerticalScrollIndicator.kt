package com.project.habithearth.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Right-edge scroll thumb driven by [scrollState] (works on Android; no desktop-only scrollbar API).
 */
@Composable
fun VerticalScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val max = scrollState.maxValue
    if (max <= 0) return

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    BoxWithConstraints(
        modifier = modifier
            .width(6.dp)
            .fillMaxHeight(),
    ) {
        val trackHeight = maxHeight
        val thumbHeight = (trackHeight * 0.25f).coerceAtLeast(32.dp).coerceAtMost(trackHeight)
        val travel = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
        val progress = (scrollState.value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        val thumbOffset = travel * progress

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackColor),
            )
            Box(
                modifier = Modifier
                    .offset(y = thumbOffset)
                    .height(thumbHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(thumbColor),
            )
        }
    }
}
