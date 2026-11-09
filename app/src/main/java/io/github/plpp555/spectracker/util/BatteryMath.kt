package io.github.plpp555.spectracker.util

import kotlin.math.abs

/**
 * バッテリー関連の単位変換。
 *
 * Android が返す生の値（µA・µAh・mV・nWh など）は、すべてここで W / Wh に変換してから表示する。
 * 端末（メーカー）によっては規定と異なる単位で値を返すことがあるため、明らかに桁がずれている値は補正する。
 */
object BatteryMath {
    /**
     * Li-ion バッテリーの公称電圧 (V)。容量(mAh/µAh) → 電力量(Wh) の換算にのみ使う内部定数で、画面には表示しない。
     * 瞬間値の電圧ではなく定数を使うことで、残量表示が電圧の揺れで変動しないようにしている。
     */
    const val NOMINAL_VOLTAGE_V = 3.85

    /** この値未満（絶対値）の電流は µA ではなく mA で報告されたものとみなす。画面 ON のアプリ実行中は 10 mA を下回ることがほぼ無いため。 */
    private const val CURRENT_MA_THRESHOLD = 10_000L

    /** この値未満の充電カウンタは µAh ではなく mAh で報告されたものとみなす。 */
    private const val CHARGE_COUNTER_MAH_THRESHOLD = 10_000L

    /** BatteryManager の「未対応」を表す値。 */
    private const val UNSUPPORTED = Long.MIN_VALUE

    /** 電流の生の値を µA にそろえる。未対応なら null。 */
    fun normalizeCurrentMicroAmps(raw: Long): Long? {
        if (raw == UNSUPPORTED) return null
        val magnitude = abs(raw)
        return if (magnitude != 0L && magnitude < CURRENT_MA_THRESHOLD) raw * 1000 else raw
    }

    /** 充電カウンタの生の値を µAh にそろえる。未対応・0 以下なら null。 */
    fun normalizeChargeCounterMicroAmpHours(raw: Long): Long? {
        if (raw == UNSUPPORTED || raw <= 0) return null
        return if (raw < CHARGE_COUNTER_MAH_THRESHOLD) raw * 1000 else raw
    }

    /** 電圧の生の値を mV にそろえる。不正値なら null。 */
    fun normalizeVoltageMilliVolts(raw: Int): Int? = when {
        raw <= 0 -> null
        raw < 100 -> raw * 1000 // V 単位で報告された
        raw > 100_000 -> raw / 1000 // µV 単位で報告された
        else -> raw
    }

    /** 瞬間電力 (W) = |電流| × 電圧。向き（充電/放電）は電流の符号ではなく充電状態から判断するため、絶対値を返す。 */
    fun powerWatts(currentMicroAmps: Long, voltageMilliVolts: Int): Double =
        abs(currentMicroAmps) / 1_000_000.0 * (voltageMilliVolts / 1000.0)

    /** 充電カウンタ (µAh) → 残りの電力量 (Wh)。 */
    fun wattHoursFromMicroAmpHours(microAmpHours: Long): Double =
        microAmpHours / 1_000_000.0 * NOMINAL_VOLTAGE_V

    /** 設計容量など mAh で与えられる容量 → 電力量 (Wh)。 */
    fun wattHoursFromMilliAmpHours(milliAmpHours: Double): Double =
        milliAmpHours / 1000.0 * NOMINAL_VOLTAGE_V

    /** ENERGY_COUNTER (nWh) → Wh。 */
    fun wattHoursFromNanoWattHours(nanoWattHours: Long): Double = nanoWattHours / 1_000_000_000.0

    /** sysfs の energy_* (µWh) → Wh。 */
    fun wattHoursFromMicroWattHours(microWattHours: Long): Double = microWattHours / 1_000_000.0

    /** 現在の残量と残量(%)から満充電時の電力量を推定する。残量が 1% 未満だと誤差が大きいため null。 */
    fun fullChargeWattHours(remainingWattHours: Double, levelPercent: Int): Double? =
        if (levelPercent in 1..100 && remainingWattHours > 0) remainingWattHours * 100.0 / levelPercent else null

    /** 劣化の目安 (%) = 満充電時の電力量 / 設計時の電力量。推定値が現実的でない範囲なら null。 */
    fun healthPercent(fullWattHours: Double, designWattHours: Double): Double? {
        if (designWattHours <= 0) return null
        val percent = fullWattHours / designWattHours * 100.0
        return percent.takeIf { it in 20.0..120.0 }
    }

    /** バッテリー温度（BatteryManager は 0.1 ℃ 単位）→ ℃。 */
    fun tenthsToCelsius(tenths: Int): Double = tenths / 10.0
}
