package com.project.habithearth.ui.map

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.ui.theme.HabitHearthTheme

private val MapMinScale = 1f
private val MapMaxScale = 4f

/** Asset path under `app/src/main/assets/`. */
private const val MapBackgroundAssetPath = "images/background_image.png"

private const val MapBuildingAssetPath = "images/building_towers.PNG"

/**
 * Cap decoded size so large map PNGs do not OOM or exceed GPU max texture size when drawn.
 */
private const val MapBackgroundMaxEdgePx = 2048
private const val MapBuildingMaxEdgePx = 256

/** Sized to sit inside one hex cell on the map art; centered on [VillageBuilding] fractions. */
private val BuildingMarkerWidth = 34.dp
private val BuildingMarkerHeight = 38.dp
private val BuildingMarkerIconSize = 16.dp

@Composable
fun MapScreen(
    onOpenBuilding: (VillageBuilding) -> Unit,
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onDispose {
                activity.requestedOrientation = previous
            }
        }
    }

    val viewport by mapViewModel.viewportState.collectAsState()

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (viewport.scale * zoomChange).coerceIn(MapMinScale, MapMaxScale)
        val nextPan = viewport.pan + panChange
        mapViewModel.updateViewport(
            scale = nextScale,
            pan = nextPan,
        )
    }

    val buildings = remember { defaultVillageBuildings() }

    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = viewport.scale
                    scaleY = viewport.scale
                    translationX = viewport.pan.x
                    translationY = viewport.pan.y
                }
                .transformable(transformableState),
        ) {
            // Map layer: fixed size; building xFraction/yFraction are 0…1 across this box (see VillageBuilding KDoc).
            BoxWithConstraints(
                Modifier
                    .size(1400.dp, 980.dp)
                    .align(Alignment.Center),
            ) {
                MapBaseBackground(Modifier.fillMaxSize())
                buildings.forEach { building ->
                    BuildingMarker(
                        building = building,
                        modifier = Modifier.offset(
                            x = maxWidth * building.xFraction - BuildingMarkerWidth / 2,
                            y = maxHeight * building.yFraction - BuildingMarkerHeight / 2,
                        ),
                        onClick = { onOpenBuilding(building) },
                    )
                }
            }
        }

        Text(
            text = "Pinch to zoom · drag to pan · tap a building to open it",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun MapBaseBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap = remember(MapBackgroundAssetPath) {
        decodeMapBackgroundBitmap(context, MapBackgroundAssetPath, MapBackgroundMaxEdgePx)
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(Color(0xFF0A3323)),
        )
    }
}

/**
 * Decodes an asset PNG with [BitmapFactory.Options.inSampleSize] so the longer side is at most [maxEdgePx].
 */
private fun decodeMapBackgroundBitmap(
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


@Composable
private fun BuildingMarker(
    building: VillageBuilding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val buildingBitmap = remember(MapBuildingAssetPath) {
        decodeMapBackgroundBitmap(context, MapBuildingAssetPath, MapBuildingMaxEdgePx)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.size(BuildingMarkerWidth, BuildingMarkerHeight),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
        ) {
            if (buildingBitmap != null) {
                Image(
                    bitmap = buildingBitmap,
                    contentDescription = "${building.name} marker",
                    modifier = Modifier.size(BuildingMarkerIconSize),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = building.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.width(BuildingMarkerWidth - 4.dp),
            )
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun MapScreenPreview() {
//    HabitHearthTheme {
//        MapScreen(
//            onOpenBuilding = {},
//            mapViewModel = MapViewModel(SavedStateHandle()),
//        )
//    }
//}
