package io.github.plpp555.spectracker.collector

import android.os.BatteryManager
import io.github.plpp555.spectracker.model.SpecGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** バッテリー表示の単位ルール（電力は W、電力量は Wh、温度は ℃）を固定するテスト。 */
class BatteryPresenterTest {
    /** 60% で放電中。1.2 A × 3.9 V = 4.68 W、現在 11.55 Wh（= 満充電 19.25 Wh の 60%）、設計容量 20 Wh。 */
    private val discharging = BatteryState(
        levelPercent = 60,
        status = BatteryManager.BATTERY_STATUS_DISCHARGING,
        plugged = 0,
        voltageMilliVolts = 3900,
        currentMicroAmps = -1_200_000,
        averageCurrentMicroAmps = -1_000_000,
        remainingWattHours = 11.55,
        designWattHours = 20.0,
        temperatureTenthsCelsius = 315,
        health = BatteryManager.BATTERY_HEALTH_GOOD,
        technology = "Li-ion",
        cycleCount = 120,
        maxChargerWatts = null,
        powerSaveMode = false,
        stateOfHealthPercent = null,
        present = true,
    )

    private fun rows(state: BatteryState): Map<String, String> =
        BatteryPresenter.groups(state).flatMap { it.items }.associate { it.label to it.value }

    @Test
    fun `放電中の電力と電力量はWとWhで表示される`() {
        val rows = rows(discharging)
        assertEquals("放電中 4.68 W", rows["充放電の電力"])
        assertEquals("3.90 W", rows["平均の電力"]) // 1.0 A × 3.9 V
        assertEquals("11.55 Wh", rows["現在の電力量"])
        assertEquals("19.25 Wh", rows["満充電時の電力量（推定）"])
        assertEquals("20.00 Wh", rows["設計容量（推定）"])
        assertEquals("96.3%", rows["劣化の目安（満充電時 / 設計容量）"])
    }

    @Test
    fun `温度は摂氏で表示される`() {
        assertEquals("31.5 ℃", rows(discharging)["温度"])
    }

    @Test
    fun `残量と状態`() {
        val rows = rows(discharging)
        assertEquals("60%", rows["残量"])
        assertEquals("放電中", rows["充電状態"])
        assertEquals("接続なし（バッテリー駆動）", rows["電源"])
        assertEquals("良好", rows["健康状態"])
        assertEquals("120 回", rows["充放電サイクル数"])
    }

    @Test
    fun `充電中は電流の符号に関係なく充電として電力を表示する`() {
        val negative = discharging.copy(status = BatteryManager.BATTERY_STATUS_CHARGING, currentMicroAmps = -2_000_000)
        val positive = negative.copy(currentMicroAmps = 2_000_000)
        assertEquals("充電中 7.80 W", rows(negative)["充放電の電力"])
        assertEquals("充電中 7.80 W", rows(positive)["充放電の電力"])
    }

    @Test
    fun `電源の種類`() {
        val usb = discharging.copy(plugged = BatteryManager.BATTERY_PLUGGED_USB)
        val acAndWireless = discharging.copy(
            plugged = BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_WIRELESS,
        )
        assertEquals("USB", rows(usb)["電源"])
        assertEquals("AC 電源 + ワイヤレス", rows(acAndWireless)["電源"])
    }

    @Test
    fun `充電器の最大出力はWで表示される`() {
        val state = discharging.copy(maxChargerWatts = 20.0)
        assertEquals("20.00 W", rows(state)["充電器の最大出力"])
    }

    @Test
    fun `概要の要約もWと摂氏で表示される`() {
        assertEquals("60%｜放電中｜放電中 4.68 W｜31.5 ℃", BatteryPresenter.summary(discharging))
    }

    @Test
    fun `取得できない項目は取得不可になり落ちない`() {
        val empty = BatteryState(
            levelPercent = null,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = 0,
            voltageMilliVolts = null,
            currentMicroAmps = null,
            averageCurrentMicroAmps = null,
            remainingWattHours = null,
            designWattHours = null,
            temperatureTenthsCelsius = null,
            health = null,
            technology = null,
            cycleCount = null,
            maxChargerWatts = null,
            powerSaveMode = null,
            stateOfHealthPercent = null,
            present = null,
        )
        val rows = rows(empty)
        assertEquals("取得不可", rows["充放電の電力"])
        assertEquals("取得不可", rows["現在の電力量"])
        assertEquals("取得不可", rows["温度"])
        assertNotNull(BatteryPresenter.summary(empty)) // 状態「不明」は残る
    }

    @Test
    fun `バッテリーの表示にW系と摂氏以外の単位が混ざらない`() {
        val states = listOf(
            discharging,
            discharging.copy(status = BatteryManager.BATTERY_STATUS_CHARGING, currentMicroAmps = 3_000_000, maxChargerWatts = 25.0),
            discharging.copy(status = BatteryManager.BATTERY_STATUS_FULL, levelPercent = 100),
            discharging.copy(stateOfHealthPercent = 92),
        )
        for (state in states) {
            assertNoForbiddenUnits(BatteryPresenter.groups(state))
            BatteryPresenter.summary(state)?.let { assertNoForbiddenUnits(it) }
        }
    }

    @Test
    fun `電力の値は必ずWで終わり 電力量はWhで終わる`() {
        val rows = rows(discharging.copy(maxChargerWatts = 18.0))
        assertTrue(rows.getValue("充放電の電力").endsWith(" W"))
        assertTrue(rows.getValue("平均の電力").endsWith(" W"))
        assertTrue(rows.getValue("充電器の最大出力").endsWith(" W"))
        assertTrue(rows.getValue("現在の電力量").endsWith(" Wh"))
        assertTrue(rows.getValue("満充電時の電力量（推定）").endsWith(" Wh"))
        assertTrue(rows.getValue("設計容量（推定）").endsWith(" Wh"))
        assertTrue(rows.getValue("温度").endsWith(" ℃"))
    }

    private fun assertNoForbiddenUnits(groups: List<SpecGroup>) {
        for (item in groups.flatMap { it.items }) assertNoForbiddenUnits("${item.label}: ${item.value}")
    }

    /** mA・mAh・µA・µAh・V・mV・°C・°F・K が含まれていないこと。 */
    private fun assertNoForbiddenUnits(text: String) {
        val forbidden = listOf(
            Regex("""\d\s*m?A(h)?\b"""),
            Regex("""\d\s*µA(h)?"""),
            Regex("""\d\s*m?V\b"""),
            Regex("""°[CF]"""),
            Regex("""\d\s*K\b"""),
        )
        for (pattern in forbidden) {
            assertFalse("禁止された単位 ${pattern.pattern} が含まれる: $text", pattern.containsMatchIn(text))
        }
    }
}
