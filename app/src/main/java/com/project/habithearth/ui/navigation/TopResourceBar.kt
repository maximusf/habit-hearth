package com.project.habithearth.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopResourceBar(
    strengthGems: Int = 0,
    wisdomGems: Int = 0,
    vitalityGems: Int = 0,
    spiritGems: Int = 0,
    coins: Int = 0,
    xpProgress: Float = 0.3f,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ResourceStat("STR", strengthGems)
                    ResourceStat("WIS", wisdomGems)
                    ResourceStat("VIT", vitalityGems)
                    ResourceStat("SPI", spiritGems)
                }
                // Coins should sit below strength (instead of next to spirit).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ResourceStat("COIN", coins)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { xpProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun ResourceStat(label: String, amount: Int) {
    Text(
        text = "$label $amount",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Resource bar with a menu strip directly below it; hamburger is top-start aligned.
 */
@Composable
fun TopChromeWithMenu(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    strengthGems: Int = 0,
    wisdomGems: Int = 0,
    vitalityGems: Int = 0,
    spiritGems: Int = 0,
    coins: Int = 0,
    xpProgress: Float = 0.32f,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TopResourceBar(
            strengthGems = strengthGems,
            wisdomGems = wisdomGems,
            vitalityGems = vitalityGems,
            spiritGems = spiritGems,
            coins = coins,
            xpProgress = xpProgress,
        )
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open menu",
                    )
                }
            }
        }
    }
}
