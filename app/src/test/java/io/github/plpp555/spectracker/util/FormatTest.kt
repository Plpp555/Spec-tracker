package io.github.plpp555.spectracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun `電力はWで表示される`() {
        assertEquals("6.00 W", Format.watts(6.0))
        assertEquals("15.40 Wh", Format.wattHours(15.4))
    }

    @Test
    fun `温度は摂氏で表示される`() {
        assertEquals("36.5 ℃", Format.celsius(36.54))
    }

    @Test
    fun `バッテリーの表示にW系以外の単位が混ざらない`() {
        val battery = listOf(Format.watts(12.3), Format.wattHours(15.4), Format.celsius(30.0))
        val forbidden = listOf("mA", "mAh", "µA", "µAh", " V", "mV", "°F", "K")
        for (text in battery) {
            for (unit in forbidden) {
                assertFalse("「$text」に禁止単位「$unit」が含まれている", text.contains(unit))
            }
        }
        assertTrue(battery[0].endsWith(" W"))
        assertTrue(battery[1].endsWith(" Wh"))
        assertTrue(battery[2].endsWith(" ℃"))
    }

    @Test
    fun `容量は10進接頭辞で表示される`() {
        assertEquals("999 B", Format.bytes(999))
        assertEquals("1.50 KB", Format.bytes(1500))
        assertEquals("1.50 GB", Format.bytes(1_500_000_000))
        assertEquals("128.00 GB", Format.bytes(128_000_000_000))
    }

    @Test
    fun `周波数の表示`() {
        assertEquals("1.80 GHz", Format.frequencyKHz(1_800_000))
        assertEquals("600 MHz", Format.frequencyKHz(600_000))
    }

    @Test
    fun `経過時間の表示`() {
        assertEquals("5分", Format.duration(5 * 60_000L))
        assertEquals("2時間 3分", Format.duration((2 * 60 + 3) * 60_000L))
        assertEquals("1日 2時間 3分", Format.duration(((24 + 2) * 60 + 3) * 60_000L))
    }

    @Test
    fun `thermal zone の値を摂氏に変換する`() {
        assertEquals(36.5, ThermalMath.zoneRawToCelsius(36_500)!!, 1e-9)
        assertEquals(36.0, ThermalMath.zoneRawToCelsius(36)!!, 1e-9)
        assertNull(ThermalMath.zoneRawToCelsius(-274_000))
        assertNull(ThermalMath.zoneRawToCelsius(900_000))
    }

    @Test
    fun `CPUコア名を判定する`() {
        assertEquals("Cortex-X4", CpuNames.resolve(0x41, 0xd82))
        assertEquals("Cortex-A520", CpuNames.resolve(0x41, 0xd80))
        assertNull(CpuNames.resolve(0x99, 0x123))
        assertEquals("implementer 0x99 / part 0x123", CpuNames.describe(0x99, 0x123))
    }
}
