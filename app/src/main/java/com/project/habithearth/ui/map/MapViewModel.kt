package com.project.habithearth.ui.map

import androidx.compose.ui.geometry.Offset
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

    private companion object {
        const val KEY_SCALE = "map_scale"
        const val KEY_PAN_X = "map_pan_x"
        const val KEY_PAN_Y = "map_pan_y"
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_PAN_X = 0f
        const val DEFAULT_PAN_Y = 0f
    }
}
