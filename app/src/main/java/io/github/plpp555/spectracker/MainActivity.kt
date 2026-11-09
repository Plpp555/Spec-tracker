package io.github.plpp555.spectracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.plpp555.spectracker.ui.SpecApp
import io.github.plpp555.spectracker.ui.SpecTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SpecTrackerTheme {
                SpecApp()
            }
        }
    }
}
