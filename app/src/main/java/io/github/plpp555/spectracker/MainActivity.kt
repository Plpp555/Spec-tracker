package io.github.plpp555.spectracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import io.github.plpp555.spectracker.ui.SpecApp
import io.github.plpp555.spectracker.ui.SpecTrackerTheme
import io.github.plpp555.spectracker.util.DeviceEdition
import io.github.plpp555.spectracker.util.EditionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 端末名の判定（getprop の実行を含む）はメインスレッドの外で行う。完了するまでは何も描画しない。
            val edition by produceState<EditionInfo?>(initialValue = null) {
                value = withContext(Dispatchers.Default) { DeviceEdition.current() }
            }
            edition?.let { info ->
                SpecTrackerTheme(
                    darkTheme = info.isUltra || isSystemInDarkTheme(),
                    dynamicColor = !info.isUltra,
                ) {
                    SpecApp(info)
                }
            }
        }
    }
}
