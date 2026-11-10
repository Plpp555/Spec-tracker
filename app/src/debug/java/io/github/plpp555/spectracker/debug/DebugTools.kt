package io.github.plpp555.spectracker.debug

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.plpp555.spectracker.util.EditionInfo
import io.github.plpp555.spectracker.util.EditionMode

/**
 * デバッグ版だけに入る開発用ツール。公開版には src/release の空実装が入るため、この UI もコードも含まれない。
 */
object DebugTools {
    private const val PREFS = "debug_tools"
    private const val KEY_EDITION_MODE = "edition_mode"

    fun loadEditionMode(context: Context): EditionMode =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EDITION_MODE, null)
            ?.let { name -> EditionMode.entries.firstOrNull { it.name == name } }
            ?: EditionMode.AUTO

    fun saveEditionMode(context: Context, mode: EditionMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_EDITION_MODE, mode.name).apply()
    }

    /** 画面上部のレンチボタン。押すと、Ultra / 特装版の演出を模擬するメニューが開く。 */
    @Composable
    fun EditionSwitcher(mode: EditionMode, real: EditionInfo, onSelect: (EditionMode) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Build, contentDescription = "デバッグ: 演出の切り替え")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "デバッグ: 演出の切り替え",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = "実機の判定: ${describe(real)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            for (option in EditionMode.entries) {
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = {
                        if (option == mode) Icon(Icons.Default.Check, contentDescription = "選択中")
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }

    private fun describe(info: EditionInfo): String = when {
        info.isUltra && info.editionName != null -> "Ultra 版 + 特装版（${info.editionName}）"
        info.isUltra -> "Ultra 版"
        info.editionName != null -> "特装版（${info.editionName}）"
        else -> "通常"
    }
}
