package io.github.plpp555.spectracker.collector

import android.os.BatteryManager
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.BatteryMath
import io.github.plpp555.spectracker.util.Format

/** 端末から読み取った生のバッテリー情報（単位変換前）。Android に依存しないので JVM 単体テストで検証できる。 */
data class BatteryState(
    val levelPercent: Int?,
    val status: Int,
    val plugged: Int,
    /** mV にそろえた電圧。電力 (W) を求めるためだけに使い、画面には表示しない。 */
    val voltageMilliVolts: Int?,
    /** µA にそろえた電流。画面には表示せず、電力 (W) に変換して使う。 */
    val currentMicroAmps: Long?,
    val averageCurrentMicroAmps: Long?,
    val remainingWattHours: Double?,
    val designWattHours: Double?,
    /** 0.1 ℃ 単位（BatteryManager の生の値）。 */
    val temperatureTenthsCelsius: Int?,
    val health: Int?,
    val technology: String?,
    val cycleCount: Int?,
    val maxChargerWatts: Double?,
    val powerSaveMode: Boolean?,
    val stateOfHealthPercent: Int?,
    val present: Boolean?,
)

/**
 * バッテリー情報の表示文字列を組み立てる。
 *
 * 単位ルール（厳守）: 電力は W、電力量は Wh、温度は ℃。mA・mAh・V・mV などは一切表示しない。
 * 残量 (%) は単位系を持たない割合のため、そのまま表示する。
 */
object BatteryPresenter {
    fun groups(state: BatteryState): List<SpecGroup> = buildGroups {
        group("現在の状態") {
            row("残量") { state.levelPercent?.let { Format.percent(it) } }
            row("充電状態") { statusText(state.status) }
            row("電源") { pluggedText(state.plugged) }
            row("充放電の電力") { powerText(state) }
            optionalRow("平均の電力") { averagePowerText(state) }
            optionalRow("充電器の最大出力") { state.maxChargerWatts?.let { Format.watts(it) } }
            row("温度") { state.temperatureTenthsCelsius?.let { Format.celsius(BatteryMath.tenthsToCelsius(it)) } }
            optionalRow("省電力モード") { state.powerSaveMode?.let { Format.yesNo(it) } }
        }
        group("電力量（Wh）") {
            val full = fullChargeWattHours(state)
            row("現在の電力量") { state.remainingWattHours?.let { Format.wattHours(it) } }
            row("満充電時の電力量（推定）") { full?.let { Format.wattHours(it) } }
            row("設計容量（推定）") { state.designWattHours?.let { Format.wattHours(it) } }
            optionalRow("劣化の目安（満充電時 / 設計容量）") {
                val design = state.designWattHours
                if (full != null && design != null) BatteryMath.healthPercent(full, design)?.let { Format.percent(it) } else null
            }
            optionalRow("バッテリー状態 (SoH)") { state.stateOfHealthPercent?.let { Format.percent(it) } }
        }
        group("バッテリー情報") {
            row("健康状態") { healthText(state.health) }
            row("テクノロジー") { state.technology }
            optionalRow("充放電サイクル数") { state.cycleCount?.let { "$it 回" } }
            optionalRow("バッテリー搭載") { state.present?.let { Format.yesNo(it) } }
        }
    }

    /** 概要タブ用の 1 行要約。 */
    fun summary(state: BatteryState): String? =
        listOfNotNull(
            state.levelPercent?.let { Format.percent(it) },
            statusText(state.status),
            powerText(state),
            state.temperatureTenthsCelsius?.let { Format.celsius(BatteryMath.tenthsToCelsius(it)) },
        ).joinToString("｜").ifEmpty { null }

    private fun fullChargeWattHours(state: BatteryState): Double? {
        val remaining = state.remainingWattHours ?: return null
        val level = state.levelPercent ?: return null
        return BatteryMath.fullChargeWattHours(remaining, level)
    }

    /**
     * 充放電の電力 (W)。電流の符号は端末によって逆のことがあるため、向きは電流の符号ではなく
     * 充電状態から判断し、電力は絶対値で表示する。
     */
    private fun powerText(state: BatteryState): String? {
        val current = state.currentMicroAmps ?: return null
        val voltage = state.voltageMilliVolts ?: return null
        val watts = Format.watts(BatteryMath.powerWatts(current, voltage))
        return when (state.status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充電中 $watts"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放電中 $watts"
            else -> watts
        }
    }

    private fun averagePowerText(state: BatteryState): String? {
        val current = state.averageCurrentMicroAmps?.takeIf { it != 0L } ?: return null
        val voltage = state.voltageMilliVolts ?: return null
        return Format.watts(BatteryMath.powerWatts(current, voltage))
    }

    private fun statusText(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "充電中"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "放電中"
        BatteryManager.BATTERY_STATUS_FULL -> "満充電"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "充電していない"
        else -> "不明"
    }

    private fun pluggedText(plugged: Int): String {
        val sources = buildList {
            if (plugged and BatteryManager.BATTERY_PLUGGED_AC != 0) add("AC 電源")
            if (plugged and BatteryManager.BATTERY_PLUGGED_USB != 0) add("USB")
            if (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0) add("ワイヤレス")
            if (plugged and 8 != 0) add("ドック") // BATTERY_PLUGGED_DOCK (API 33)
        }
        return sources.joinToString(" + ").ifEmpty { "接続なし（バッテリー駆動）" }
    }

    private fun healthText(health: Int?): String? = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "過熱"
        BatteryManager.BATTERY_HEALTH_DEAD -> "寿命"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "過電圧"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "不明な異常"
        BatteryManager.BATTERY_HEALTH_COLD -> "低温"
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> "不明"
        else -> null
    }
}
