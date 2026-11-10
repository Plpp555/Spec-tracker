package io.github.plpp555.spectracker.debug

import android.content.Context
import androidx.compose.runtime.Composable
import io.github.plpp555.spectracker.util.EditionInfo
import io.github.plpp555.spectracker.util.EditionMode

/**
 * 公開版用の空実装。切り替えスイッチは存在せず、保存された設定も読まない（常に実機の判定どおり）。
 * debug 版の実装は src/debug にある。
 */
@Suppress("UNUSED_PARAMETER")
object DebugTools {
    fun loadEditionMode(context: Context): EditionMode = EditionMode.AUTO

    fun saveEditionMode(context: Context, mode: EditionMode) = Unit

    @Composable
    fun EditionSwitcher(mode: EditionMode, real: EditionInfo, onSelect: (EditionMode) -> Unit) = Unit
}
