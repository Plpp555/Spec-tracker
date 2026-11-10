package io.github.plpp555.spectracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EditionModeTest {
    private val real = EditionInfo(isUltra = false, editionName = null)
    private val sample = "Xiaomi Mi 10 Debug Edition"

    @Test
    fun `自動は実機の判定どおり`() {
        assertEquals(real, EditionMode.AUTO.resolve(real, sample))
        val ultra = EditionInfo(true, "X Edition")
        assertEquals(ultra, EditionMode.AUTO.resolve(ultra, sample))
    }

    @Test
    fun `各モードが演出を模擬する`() {
        val ultraReal = EditionInfo(true, "X Edition")
        assertEquals(EditionInfo.NONE, EditionMode.NORMAL.resolve(ultraReal, sample))
        assertEquals(EditionInfo(true, null), EditionMode.ULTRA.resolve(real, sample))
        assertEquals(EditionInfo(false, sample), EditionMode.EDITION.resolve(real, sample))
        assertEquals(EditionInfo(true, sample), EditionMode.ULTRA_EDITION.resolve(real, sample))
    }
}
