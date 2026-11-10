package io.github.plpp555.spectracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCatalogTest {
    @Test
    fun `Samsung は地域違いの型番も同じ機種になる`() {
        for (model in listOf("SM-S928B", "SM-S928U", "SM-S928N", "SM-S928Q", "sm-s928b")) {
            assertEquals(model, "Galaxy S24 Ultra", DeviceCatalog.marketNameFor(model))
        }
        assertEquals("Galaxy S25 Ultra", DeviceCatalog.marketNameFor("SM-S938B"))
        assertEquals("Galaxy S23 Ultra", DeviceCatalog.marketNameFor("SM-S918U1"))
    }

    @Test
    fun `日本向けの型番`() {
        assertEquals("Galaxy S24 Ultra", DeviceCatalog.marketNameFor("SC-52E"))
        assertEquals("Galaxy S24 Ultra", DeviceCatalog.marketNameFor("SCG26"))
        assertEquals("Galaxy S22 Ultra", DeviceCatalog.marketNameFor("SC-52C"))
    }

    @Test
    fun `Xiaomi は完全一致のみで、先頭が同じ別機種を巻き込まない`() {
        assertEquals("Xiaomi Mi 11 Ultra", DeviceCatalog.marketNameFor("M2102K1G"))
        assertNull(DeviceCatalog.marketNameFor("M2102K1AC")) // Mi 11 Pro
        assertEquals("Xiaomi Mi 10 Ultra", DeviceCatalog.marketNameFor("M2007J1SC"))
        assertEquals("Xiaomi 14 Ultra", DeviceCatalog.marketNameFor("24030PN60G"))
    }

    @Test
    fun `載っていない型番や空は null`() {
        assertNull(DeviceCatalog.marketNameFor("SM-S921B")) // Galaxy S24（Ultra ではない）
        assertNull(DeviceCatalog.marketNameFor("SC-51E"))
        assertNull(DeviceCatalog.marketNameFor("Pixel 9 Pro"))
        assertNull(DeviceCatalog.marketNameFor(""))
        assertNull(DeviceCatalog.marketNameFor(null))
    }

    @Test
    fun `対応表で補った販売名からUltraと判定できる`() {
        val name = DeviceCatalog.marketNameFor("SM-S928B")!!
        assertTrue(DeviceEdition.detect(listOf(name, "SM-S928B"), "samsung").isUltra)
        assertTrue(DeviceEdition.detect(listOfNotNull(DeviceCatalog.marketNameFor("SC-52E"), "SC-52E"), "samsung").isUltra)
    }
}
