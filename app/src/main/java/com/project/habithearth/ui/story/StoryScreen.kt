package com.project.habithearth.ui.story

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.habithearth.R
import com.project.habithearth.ui.theme.HabitHearthTheme

@Composable
fun StoryScreen(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                activity.requestedOrientation = previous
            }
        }
    }

    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
    ) {
        Text(
            text = "Story",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Landscape reading mode — scroll if the tale runs long.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Story illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                text = "The ember at the center of your village remembers every step you take. " +
                    "Each habit you tend adds another timber to the hearth — not all at once, " +
                    "but steady, until the whole place glows. " +
                    "This is where your chronicle will grow: moments beside the fire, " +
                    "paths through the map, and the small choices that become a life well kept.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true, device = "spec:parent=pixel_5,orientation=landscape")
//@Composable
//private fun StoryScreenPreview() {
//    HabitHearthTheme {
//        StoryScreen()
//    }
//}
