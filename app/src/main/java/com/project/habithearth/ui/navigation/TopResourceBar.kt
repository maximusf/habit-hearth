package com.project.habithearth.ui.navigation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val GemStrengthAssetPath = "images/gem_strength.png"
private const val GemWisdomAssetPath = "images/gem_wisdom.png"
private const val GemVitalityAssetPath = "images/gem_vitality.png"
private const val GemSpiritAssetPath = "images/gem_spirit.png"
private const val CoinAssetPath = "images/Coin.png"
private const val GemAssetMaxEdgePx = 128
private const val CoinAssetMaxEdgePx = 192

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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GemStat(
                        amount = strengthGems,
                        assetPath = GemStrengthAssetPath,
                        accessibilityLabel = "Strength gems",
                    )
                    GemStat(
                        amount = wisdomGems,
                        assetPath = GemWisdomAssetPath,
                        accessibilityLabel = "Wisdom gems",
                    )
                    GemStat(
                        amount = vitalityGems,
                        assetPath = GemVitalityAssetPath,
                        accessibilityLabel = "Vitality gems",
                    )
                    GemStat(
                        amount = spiritGems,
                        assetPath = GemSpiritAssetPath,
                        accessibilityLabel = "Spirit gems",
                    )
                }
                // Coins sit below strength; same image + overlaid count as gems.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GemStat(
                        amount = coins,
                        assetPath = CoinAssetPath,
                        accessibilityLabel = "Coins",
                        boxModifier = Modifier.size(
                            width = GemStatSize * 2,
                            height = GemStatSize,
                        ),
                        decodeMaxEdgePx = CoinAssetMaxEdgePx,
                    )
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

private val GemStatSize = 34.dp

/** Show 0–999 as-is; from 1000 onward use [thousands]k+ (e.g. 1000 → 1k+). */
private fun formatResourceAmountForGem(amount: Int): String {
    val n = amount.coerceAtLeast(0)
    if (n <= 999) return n.toString()
    val k = n / 1000
    return "${k}k+"
}

@Composable
private fun GemStat(
    amount: Int,
    assetPath: String,
    accessibilityLabel: String,
    boxModifier: Modifier = Modifier.size(GemStatSize),
    decodeMaxEdgePx: Int = GemAssetMaxEdgePx,
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath, decodeMaxEdgePx) {
        decodeGemAssetBitmap(context, assetPath, decodeMaxEdgePx)
    }
    val displayAmount = formatResourceAmountForGem(amount)
    val fontSize = when (displayAmount.length) {
        1, 2 -> 13.sp
        3 -> 10.5.sp
        else -> 8.5.sp
    }
    val amountStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
    )
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
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = displayAmount,
            style = amountStyle,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .semantics { invisibleToUser() },
        )
    }
}

private fun decodeGemAssetBitmap(
    context: Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
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
