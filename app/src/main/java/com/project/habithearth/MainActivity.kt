package com.project.habithearth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import com.project.habithearth.ui.HabitHearthApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * enableEdgeToEdge() is the modern way to handle status bars in Android.
         * It makes the system bars transparent so your app's background color
         * can go all the way to the top and bottom.
         */
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        /**
         * Note: We removed the WindowInsetsControllerCompat block that was
         * hiding the system bars. Hiding them (Immersive Mode) prevents
         * statusBarsPadding() from working correctly in your TopResourceBar.
         */

        setContent {
            AppTheme {
                HabitHearthApp(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}