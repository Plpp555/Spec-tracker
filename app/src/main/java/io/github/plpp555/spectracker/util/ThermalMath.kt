package io.github.plpp555.spectracker.util

import kotlin.math.abs

object ThermalMath {
    /**
     * /sys/class/thermal の temp の生の値を ℃ に変換する。
     * 通常は m℃（例: 36500）だが、℃ そのもの（例: 36）で返す端末もある。
     * センサー無効を表す値（-274000 など）や、現実的でない値は null。
     */
    fun zoneRawToCelsius(raw: Long): Double? {
        val celsius = if (abs(raw) >= 1000) raw / 1000.0 else raw.toDouble()
        return celsius.takeIf { it > -50.0 && it < 200.0 }
    }
}
