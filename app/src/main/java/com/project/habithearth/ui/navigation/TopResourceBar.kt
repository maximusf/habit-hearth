package com.project.habithearth.ui.navigation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val GemStrengthAssetPath = "images/gem_strength.png"
private const val GemWisdomAssetPath = "images/gem_wisdom.png"
private const val GemVitalityAssetPath = "images/gem_vitality.png"
private const val GemSpiritAssetPath = "images/gem_spirit.png"
private const val CoinAssetPath = "images/Coin.png"
private const val GemAssetMaxEdgePx = 128
private const val CoinAssetMaxEdgePx = 256

private val GemStatSize = 34.dp

@Composable
fun TopResourceBar(
    strengthGems: Int = 0,
    wisdomGems: Int = 0,
    vitalityGems: Int = 0,
    spiritGems: Int = 0,
    coins: Int = 0,
    totalXp: Int = 0,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null, // New parameter for the hamburger
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Menu + Resources
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationIcon != null) {
                    navigationIcon()
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResourceGroup(strengthGems, wisdomGems, vitalityGems, spiritGems, coins)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ResourceGroup(strengthGems, wisdomGems, vitalityGems, spiritGems, coins)
                    }
                }
            }

            // Right Side: Level and XP
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                val level = com.project.habithearth.ui.state.levelFor(totalXp)
                val inLevel = com.project.habithearth.ui.state.xpInLevel(totalXp)
                val perLevel = com.project.habithearth.ui.state.XP_PER_LEVEL
                Text(
                    text = "Lv $level · $inLevel/$perLevel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { inLevel.toFloat() / perLevel.toFloat() },
                    modifier = Modifier.width(100.dp),
                )
            }
        }
    }
}

@Composable
private fun ResourceGroup(
    strength: Int,
    wisdom: Int,
    vitality: Int,
    spirit: Int,
    coins: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GemStat(amount = strength, assetPath = GemStrengthAssetPath, accessibilityLabel = "Strength")
        GemStat(amount = wisdom, assetPath = GemWisdomAssetPath, accessibilityLabel = "Wisdom")
        GemStat(amount = vitality, assetPath = GemVitalityAssetPath, accessibilityLabel = "Vitality")
        GemStat(amount = spirit, assetPath = GemSpiritAssetPath, accessibilityLabel = "Spirit")
        GemStat(
            amount = coins,
            assetPath = CoinAssetPath,
            accessibilityLabel = "Coins",
            boxModifier = Modifier.size(width = 70.dp, height = 70.dp),
            decodeMaxEdgePx = CoinAssetMaxEdgePx,
            baseFontSize = 18f,
            textOffset = 2.dp,
            isCoin = true
        )
    }
}

@Composable
private fun GemStat(
    amount: Int,
    assetPath: String,
    accessibilityLabel: String,
    boxModifier: Modifier = Modifier.size(GemStatSize),
    decodeMaxEdgePx: Int = GemAssetMaxEdgePx,
    baseFontSize: Float = 13f,
    textOffset: Dp = 0.dp,
    isCoin: Boolean = false
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath, decodeMaxEdgePx) {
        decodeGemAssetBitmap(context, assetPath, decodeMaxEdgePx)
    }
    val displayAmount = formatResourceAmountForGem(amount)

    val fontSize = when (displayAmount.length) {
        1, 2 -> baseFontSize.sp
        3 -> (baseFontSize * 0.85).sp
        else -> (baseFontSize * 0.7).sp
    }

    Box(
        modifier = boxModifier.semantics(mergeDescendants = true) {
            contentDescription = "$accessibilityLabel, $amount"
        },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (isCoin) ContentScale.FillBounds else ContentScale.Fit,
            )
        }
        Text(
            text = displayAmount,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
            ),
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .padding(start = if (isCoin) 14.dp else 0.dp)
                .padding(bottom = textOffset)
                .semantics { invisibleToUser() },
        )
    }
}

private fun formatResourceAmountForGem(amount: Int): String {
    val n = amount.coerceAtLeast(0)
    if (n <= 999) return n.toString()
    return "${n / 1000}k+"
}

private fun decodeGemAssetBitmap(context: Context, assetPath: String, maxEdgePx: Int): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / sample > maxEdgePx) sample *= 2
    return sample
}

/**
 * Modernized Chrome: Hamburger menu is now inline with the resource stats.
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
    totalXp: Int = 0,
) {
    TopResourceBar(
        modifier = modifier,
        strengthGems = strengthGems,
        wisdomGems = wisdomGems,
        vitalityGems = vitalityGems,
        spiritGems = spiritGems,
        coins = coins,
        totalXp = totalXp,
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}