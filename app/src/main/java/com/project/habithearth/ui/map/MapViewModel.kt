package com.project.habithearth.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapViewportState(
    val scale: Float,
    val pan: Offset,
)

class MapViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _viewportState = MutableStateFlow(
        MapViewportState(
            scale = savedStateHandle[KEY_SCALE] ?: DEFAULT_SCALE,
            pan = Offset(
                x = savedStateHandle[KEY_PAN_X] ?: DEFAULT_PAN_X,
                y = savedStateHandle[KEY_PAN_Y] ?: DEFAULT_PAN_Y,
            ),
        ),
    )
    val viewportState: StateFlow<MapViewportState> = _viewportState.asStateFlow()

    fun updateViewport(scale: Float, pan: Offset) {
        _viewportState.value = MapViewportState(scale = scale, pan = pan)
        savedStateHandle[KEY_SCALE] = scale
        savedStateHandle[KEY_PAN_X] = pan.x
        savedStateHandle[KEY_PAN_Y] = pan.y
    }

    /** If the map is still at factory default (no saved pan/zoom), zoom to the main village hub. */
    fun seedInitialViewportIfDefault(hub: MapViewportState) {
        val current = _viewportState.value
        if (current.scale != DEFAULT_SCALE || current.pan != Offset(DEFAULT_PAN_X, DEFAULT_PAN_Y)) {
            return
        }
        updateViewport(hub.scale, hub.pan)
    }

    companion object {
        private const val KEY_SCALE = "map_scale"
        private const val KEY_PAN_X = "map_pan_x"
        private const val KEY_PAN_Y = "map_pan_y"
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_PAN_X = 0f
        const val DEFAULT_PAN_Y = 0f

        private val MapContentWidthDp = 1400.dp
        private val MapContentHeightDp = 980.dp

        /**
         * [MapScreen] uses a 1400×980 dp layer; [graphicsLayer] scale + pan match [transformable] (pixel offsets).
         * Centers the bounding box of [MainHubBuildingIds] and zooms in (~2.35×).
         */
        fun initialViewportForMainBuildings(density: Density): MapViewportState {
            val buildings = defaultVillageBuildings().filter { it.id in MainHubBuildingIds }
            check(buildings.size == MainHubBuildingIds.size) {
                "Main hub building ids must all exist in defaultVillageBuildings()"
            }
            val minX = buildings.minOf { it.xFraction }
            val maxX = buildings.maxOf { it.xFraction }
            val minY = buildings.minOf { it.yFraction }
            val maxY = buildings.maxOf { it.yFraction }
            val pad = 0.04f
            val cx = ((minX + maxX) / 2f).coerceIn(pad, 1f - pad)
            val cy = ((minY + maxY) / 2f).coerceIn(pad, 1f - pad)

            val scale = 2.35f

            val mapW = with(density) { MapContentWidthDp.toPx() }
            val mapH = with(density) { MapContentHeightDp.toPx() }
            val dx = (cx - 0.5f) * mapW
            val dy = (cy - 0.5f) * mapH
            val panX = -dx * scale
            val panY = -dy * scale
            return MapViewportState(scale = scale, pan = Offset(panX, panY))
        }
    }
}
