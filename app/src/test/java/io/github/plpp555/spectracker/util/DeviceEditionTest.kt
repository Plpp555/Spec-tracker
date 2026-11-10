package io.github.plpp555.spectracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceEditionTest {
    private fun detect(vararg names: String, maker: String = "samsung") = DeviceEdition.detect(names.toList(), maker)

    @Test
    fun `販売名にUltraを含めばUltra`() {
        assertTrue(detect("Galaxy S24 Ultra").isUltra)
        assertTrue(detect("Mi 10 Ultra").isUltra)
        assertTrue(detect("Mi10Ultra").isUltra)
        assertTrue(detect("galaxy s24 ULTRA").isUltra)
    }

    @Test
    fun `Ultraを部分的に含む別の単語はUltraではない`() {
        assertFalse(detect("Ultrabook X1").isUltra)
        assertFalse(detect("Pixel 9 Pro").isUltra)
        assertFalse(detect("SM-S928B").isUltra)
    }

    @Test
    fun `特装版Editionは端末名つきで検出される`() {
        assertEquals("Xiaomi Mi 10 Ultra Anniversary Edition", detect("Mi 10 Ultra Anniversary Edition", maker = "Xiaomi").editionName)
        assertEquals("Samsung Galaxy Z Flip Thom Browne Edition", detect("Galaxy Z Flip Thom Browne Edition").editionName)
        assertEquals("Phone Limited Edition", detect("Phone Limited Edition", maker = "").editionName)
    }

    @Test
    fun `メーカー名が既に含まれていれば重ねない`() {
        assertEquals("Xiaomi Mi 10 Ultra Special Edition", detect("Xiaomi Mi 10 Ultra Special Edition", maker = "Xiaomi").editionName)
    }

    @Test
    fun `地域エディションは特装版として扱わない`() {
        assertNull(detect("Galaxy S23 Japan Edition").editionName)
        assertNull(detect("Galaxy S23 JapanEdition").editionName)
        assertNull(detect("Redmi Note 12 Global Edition").editionName)
        assertNull(detect("Phone Hong Kong Edition").editionName)
        assertNull(detect("Phone Middle East Edition").editionName)
    }

    @Test
    fun `UltraかつEditionなら両方`() {
        val info = detect("Mi 10 Ultra Transparent Edition", maker = "Xiaomi")
        assertTrue(info.isUltra)
        assertEquals("Xiaomi Mi 10 Ultra Transparent Edition", info.editionName)
    }

    @Test
    fun `UltraでもJapanEditionなら演出はUltraのみ`() {
        val info = detect("Galaxy S24 Ultra Japan Edition")
        assertTrue(info.isUltra)
        assertNull(info.editionName)
    }

    @Test
    fun `該当なしと空の候補`() {
        assertEquals(EditionInfo.NONE, detect("Pixel 9 Pro"))
        assertEquals(EditionInfo.NONE, detect("", "  "))
    }

    @Test
    fun `販売名で判定できなくても候補の後ろの型番で判定する`() {
        assertTrue(detect("Galaxy A15", "Galaxy S24 Ultra").isUltra)
    }
}
