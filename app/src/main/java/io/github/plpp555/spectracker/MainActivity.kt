package io.github.plpp555.spectracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.plpp555.spectracker.debug.DebugTools
import io.github.plpp555.spectracker.ui.SpecApp
import io.github.plpp555.spectracker.ui.SpecTrackerTheme
import io.github.plpp555.spectracker.util.DeviceEdition
import io.github.plpp555.spectracker.util.EditionInfo
import io.github.plpp555.spectracker.util.EditionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 端末名の判定（getprop の実行を含む）はメインスレッドの外で行う。完了するまでは何も描画しない。
            val real by produceState<EditionInfo?>(initialValue = null) {
                value = withContext(Dispatchers.Default) { DeviceEdition.current() }
            }
            // デバッグ版だけ、Ultra / 特装版の演出を切り替えられる（公開版では常に AUTO）
            var mode by remember { mutableStateOf(DebugTools.loadEditionMode(applicationContext)) }
            var splashKey by rememberSaveable { mutableIntStateOf(0) }

            real?.let { realInfo ->
                val sampleName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} Debug Edition"
                val info = mode.resolve(realInfo, sampleName)
                SpecTrackerTheme(
                    darkTheme = info.isUltra || isSystemInDarkTheme(),
                    dynamicColor = !info.isUltra,
                ) {
                    SpecApp(
                        edition = info,
                        splashKey = splashKey,
                        debugActions = {
                            DebugTools.EditionSwitcher(mode, realInfo) { selected ->
                                mode = selected
                                DebugTools.saveEditionMode(applicationContext, selected)
                                splashKey++ // 特装版を選び直したときもポップアップを再生する
                            }
                        },
                    )
                }
            }
        }
    }
}
