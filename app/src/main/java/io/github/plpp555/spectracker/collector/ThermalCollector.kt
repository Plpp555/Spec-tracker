package io.github.plpp555.spectracker.collector

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.SpecItem
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.BatteryMath
import io.github.plpp555.spectracker.util.Format
import io.github.plpp555.spectracker.util.ThermalMath
import io.github.plpp555.spectracker.util.readFileOrNull
import io.github.plpp555.spectracker.util.readLongOrNull

/** 温度。すべて ℃ で表示する。 */
object ThermalCollector {
    private const val THERMAL_DIR = "/sys/class/thermal"

    /** 温度センサー 1 つ分。celsius は変換済み（℃）。 */
    internal class Zone(val index: Int, val type: String, val celsius: Double)

    fun collect(context: Context): List<SpecGroup> = buildGroups {
        group("バッテリー") {
            row("バッテリー温度") {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                    ?.takeIf { it != Int.MIN_VALUE }
                    ?.let { Format.celsius(BatteryMath.tenthsToCelsius(it)) }
            }
        }
        group("端末の熱状態") {
            row("サーマルステータス") {
                if (Build.VERSION.SDK_INT >= 29) {
                    thermalStatusText(context.getSystemService(PowerManager::class.java).currentThermalStatus)
                } else {
                    null
                }
            }
        }
        group("温度センサー（thermal zone）") {
            for (item in zoneRows(readZones())) row(item.label, item.value)
        }
    }

    /** thermal zone の一覧を表示用の行にする。温度は必ず ℃。同名のセンサーは番号で区別する。 */
    internal fun zoneRows(zones: List<Zone>): List<SpecItem> {
        if (zones.isEmpty()) {
            return listOf(SpecItem("状態", "この端末では、アプリから読み取れる温度センサーはありません"))
        }
        val duplicated = zones.groupingBy { it.type }.eachCount().filterValues { it > 1 }.keys
        return zones.map { zone ->
            val label = if (zone.type in duplicated) "${zone.type} (#${zone.index})" else zone.type
            SpecItem(label, Format.celsius(zone.celsius))
        }
    }

    private fun readZones(): List<Zone> =
        (0 until 100).mapNotNull { index ->
            val dir = "$THERMAL_DIR/thermal_zone$index"
            val celsius = readLongOrNull("$dir/temp")?.let { ThermalMath.zoneRawToCelsius(it) } ?: return@mapNotNull null
            Zone(index, readFileOrNull("$dir/type") ?: "zone$index", celsius)
        }

    private fun thermalStatusText(status: Int): String? = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "正常（制限なし）"
        PowerManager.THERMAL_STATUS_LIGHT -> "軽度（わずかに性能を制限）"
        PowerManager.THERMAL_STATUS_MODERATE -> "中程度（性能を制限中）"
        PowerManager.THERMAL_STATUS_SEVERE -> "重度（大きく性能を制限中）"
        PowerManager.THERMAL_STATUS_CRITICAL -> "危険（機能を大きく停止中）"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "緊急（主要機能を停止中）"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "シャットダウン直前"
        else -> null
    }
}
