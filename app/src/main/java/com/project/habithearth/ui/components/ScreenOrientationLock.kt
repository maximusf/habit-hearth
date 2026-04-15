package com.project.habithearth.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return
    val previousOrientation = rememberSaveable { activity.requestedOrientation }

    DisposableEffect(activity, orientation, previousOrientation) {
        activity.requestedOrientation = orientation

        onDispose {
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
}
