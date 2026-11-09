package io.github.plpp555.spectracker.util

import io.github.plpp555.spectracker.model.UNAVAILABLE
import java.util.Locale

/**
 * 表示用フォーマッタ。
 *
 * 単位ルール（厳守）:
 *  - バッテリーに関する数値は W（電力）/ Wh（電力量）に統一する。mA・mAh・V などは表示しない。
 *  - 温度は ℃ に統一する。°F・K などは表示しない。
 */
object Format {
    private fun f(pattern: String, vararg args: Any): String = String.format(Locale.ROOT, pattern, *args)

    /** 電力 (W)。 */
    fun watts(watts: Double): String = f("%.2f W", watts)

    /** 電力量 (Wh)。 */
    fun wattHours(wattHours: Double): String = f("%.2f Wh", wattHours)

    /** 温度 (℃)。 */
    fun celsius(celsius: Double): String = f("%.1f ℃", celsius)

    /** 容量。10 進接頭辞（1 GB = 1,000,000,000 B）。 */
    fun bytes(value: Long): String {
        if (value < 0) return UNAVAILABLE
        if (value < 1000) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB", "PB")
        var v = value.toDouble()
        var i = -1
        while (v >= 1000 && i < units.lastIndex) {
            v /= 1000
            i++
        }
        return f("%.2f %s", v, units[i])
    }

    /** 周波数（入力は kHz）。 */
    fun frequencyKHz(kHz: Long): String =
        if (kHz >= 1_000_000) f("%.2f GHz", kHz / 1_000_000.0) else f("%d MHz", kHz / 1000)

    /** 割合 (%)。 */
    fun percent(value: Double): String = f("%.1f%%", value)

    fun percent(value: Int): String = "$value%"

    /** 経過時間。 */
    fun duration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return buildString {
            if (days > 0) append("${days}日 ")
            if (days > 0 || hours > 0) append("${hours}時間 ")
            append("${minutes}分")
        }
    }

    fun supported(value: Boolean): String = if (value) "対応" else "非対応"

    fun yesNo(value: Boolean): String = if (value) "はい" else "いいえ"

    fun decimal(value: Double, digits: Int = 2): String = f("%.${digits}f", value)
}
