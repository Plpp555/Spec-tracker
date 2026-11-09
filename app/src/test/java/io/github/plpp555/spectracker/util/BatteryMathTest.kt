package io.github.plpp555.spectracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMathTest {
    @Test
    fun `電力は電流と電圧の積でWになる`() {
        // 1.5 A × 4.0 V = 6.0 W
        assertEquals(6.0, BatteryMath.powerWatts(1_500_000, 4000), 1e-9)
    }

    @Test
    fun `電力は電流の符号に依存しない`() {
        assertEquals(
            BatteryMath.powerWatts(800_000, 3900),
            BatteryMath.powerWatts(-800_000, 3900),
            1e-9,
        )
    }

    @Test
    fun `未対応の電流はnull`() {
        assertNull(BatteryMath.normalizeCurrentMicroAmps(Long.MIN_VALUE))
    }

    @Test
    fun `マイクロアンペアの電流はそのまま`() {
        assertEquals(-450_000L, BatteryMath.normalizeCurrentMicroAmps(-450_000))
    }

    @Test
    fun `ミリアンペアで報告された電流は補正される`() {
        assertEquals(-800_000L, BatteryMath.normalizeCurrentMicroAmps(-800))
        assertEquals(0L, BatteryMath.normalizeCurrentMicroAmps(0))
    }

    @Test
    fun `電圧の桁ずれを補正する`() {
        assertEquals(4000, BatteryMath.normalizeVoltageMilliVolts(4000))
        assertEquals(4000, BatteryMath.normalizeVoltageMilliVolts(4))
        assertEquals(4000, BatteryMath.normalizeVoltageMilliVolts(4_000_000))
        assertNull(BatteryMath.normalizeVoltageMilliVolts(0))
    }

    @Test
    fun `充電カウンタの桁ずれを補正する`() {
        assertEquals(2_500_000L, BatteryMath.normalizeChargeCounterMicroAmpHours(2_500_000))
        assertEquals(2_500_000L, BatteryMath.normalizeChargeCounterMicroAmpHours(2_500))
        assertNull(BatteryMath.normalizeChargeCounterMicroAmpHours(0))
    }

    @Test
    fun `電力量はWhに換算される`() {
        // 4.0 Ah × 3.85 V = 15.4 Wh
        assertEquals(15.4, BatteryMath.wattHoursFromMicroAmpHours(4_000_000), 1e-9)
        assertEquals(15.4, BatteryMath.wattHoursFromMilliAmpHours(4000.0), 1e-9)
        assertEquals(12.0, BatteryMath.wattHoursFromNanoWattHours(12_000_000_000), 1e-9)
        assertEquals(12.0, BatteryMath.wattHoursFromMicroWattHours(12_000_000), 1e-9)
    }

    @Test
    fun `満充電時の電力量を残量から推定する`() {
        assertEquals(15.0, BatteryMath.fullChargeWattHours(7.5, 50)!!, 1e-9)
        assertNull(BatteryMath.fullChargeWattHours(7.5, 0))
    }

    @Test
    fun `劣化の目安は現実的な範囲のみ返す`() {
        assertEquals(90.0, BatteryMath.healthPercent(13.5, 15.0)!!, 1e-9)
        assertNull(BatteryMath.healthPercent(1.0, 15.0))
        assertNull(BatteryMath.healthPercent(13.5, 0.0))
    }

    @Test
    fun `バッテリー温度は摂氏に変換される`() {
        assertEquals(36.5, BatteryMath.tenthsToCelsius(365), 1e-9)
    }
}
