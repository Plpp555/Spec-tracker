package io.github.plpp555.spectracker.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPresentationTest {
    @Test
    fun `温度センサーの値は摂氏で表示される`() {
        val rows = ThermalCollector.zoneRows(
            listOf(
                ThermalCollector.Zone(0, "cpu-0-0-usr", 41.25),
                ThermalCollector.Zone(1, "battery", 31.5),
            ),
        )
        assertEquals("41.3 ℃", rows[0].value) // 小数 1 桁に丸める
        assertEquals("31.5 ℃", rows[1].value)
        assertTrue(rows.all { it.value.endsWith(" ℃") })
        assertFalse(rows.any { Regex("""°F|\d\s*K\b""").containsMatchIn(it.value) })
    }

    @Test
    fun `同名のセンサーは番号で区別される`() {
        val rows = ThermalCollector.zoneRows(
            listOf(
                ThermalCollector.Zone(3, "cpu", 40.0),
                ThermalCollector.Zone(7, "cpu", 42.0),
                ThermalCollector.Zone(9, "gpu", 38.0),
            ),
        )
        assertEquals(listOf("cpu (#3)", "cpu (#7)", "gpu"), rows.map { it.label })
    }

    @Test
    fun `読み取れるセンサーが無い場合は案内を表示する`() {
        val rows = ThermalCollector.zoneRows(emptyList())
        assertEquals(1, rows.size)
        assertTrue(rows[0].value.contains("読み取れる温度センサーはありません"))
    }
}
