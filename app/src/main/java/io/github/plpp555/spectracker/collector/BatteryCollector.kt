package io.github.plpp555.spectracker.collector

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.util.BatteryMath
import io.github.plpp555.spectracker.util.readLongOrNull

/**
 * バッテリー情報を端末から読み取る。表示用の文字列（W / Wh / ℃ への統一）は [BatteryPresenter] が担当する。
 */
object BatteryCollector {
    private const val POWER_SUPPLY = "/sys/class/power_supply/battery"

    /** BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH（Android 14 以降。SDK 上は非公開定数のため値を直接指定）。 */
    private const val BATTERY_PROPERTY_STATE_OF_HEALTH = 10

    fun collect(context: Context): List<SpecGroup> = BatteryPresenter.groups(readState(context))

    fun summary(context: Context): String? = BatteryPresenter.summary(readState(context))

    fun readState(context: Context): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager = context.getSystemService(BatteryManager::class.java)

        val level = intent?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) raw * 100 / scale else null
        } ?: manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }

        // 現在の電力量: ENERGY_COUNTER（nWh）が使えればそれを、無ければ充電カウンタ（µAh）から換算する
        val energyCounter = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        val chargeCounter = BatteryMath.normalizeChargeCounterMicroAmpHours(
            manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
        )
        val remaining = energyCounter.takeIf { it > 0 && it != Long.MIN_VALUE }
            ?.let { BatteryMath.wattHoursFromNanoWattHours(it) }
            ?: chargeCounter?.let { BatteryMath.wattHoursFromMicroAmpHours(it) }

        return BatteryState(
            levelPercent = level,
            status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
            voltageMilliVolts = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                ?.let { BatteryMath.normalizeVoltageMilliVolts(it) },
            currentMicroAmps = BatteryMath.normalizeCurrentMicroAmps(
                manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            ),
            averageCurrentMicroAmps = BatteryMath.normalizeCurrentMicroAmps(
                manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
            ),
            remainingWattHours = remaining,
            designWattHours = designWattHours(context),
            temperatureTenthsCelsius = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeIf { it != Int.MIN_VALUE },
            health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
            cycleCount = intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it >= 0 }
                ?: readLongOrNull("$POWER_SUPPLY/cycle_count")?.takeIf { it >= 0 }?.toInt(),
            maxChargerWatts = maxChargerWatts(intent),
            powerSaveMode = try {
                context.getSystemService(PowerManager::class.java).isPowerSaveMode
            } catch (e: Exception) {
                null
            },
            stateOfHealthPercent = if (Build.VERSION.SDK_INT >= 34) {
                manager.getIntProperty(BATTERY_PROPERTY_STATE_OF_HEALTH).takeIf { it in 1..100 }
            } else {
                null
            },
            present = intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
        )
    }

    /** 充電器が申告している最大出力 (W)。AOSP が ACTION_BATTERY_CHANGED に載せる値（µA・µV）から求める。 */
    private fun maxChargerWatts(intent: Intent?): Double? {
        val microAmps = intent?.getIntExtra("max_charging_current", 0)?.takeIf { it > 0 } ?: return null
        val microVolts = intent.getIntExtra("max_charging_voltage", 0).takeIf { it > 0 } ?: return null
        return microAmps / 1_000_000.0 * (microVolts / 1_000_000.0)
    }

    /** 設計容量 (Wh)。sysfs → 内部プロファイルの順に調べる。 */
    private fun designWattHours(context: Context): Double? {
        readLongOrNull("$POWER_SUPPLY/energy_full_design")?.takeIf { it > 0 }
            ?.let { return BatteryMath.wattHoursFromMicroWattHours(it) }
        readLongOrNull("$POWER_SUPPLY/charge_full_design")
            ?.let { BatteryMath.normalizeChargeCounterMicroAmpHours(it) }
            ?.let { return BatteryMath.wattHoursFromMicroAmpHours(it) }
        return powerProfileCapacityMilliAmpHours(context)?.let { BatteryMath.wattHoursFromMilliAmpHours(it) }
    }

    /** 端末の内部プロファイルに記録された電池容量 (mAh)。非公開 API のため、無い端末では null。 */
    @SuppressLint("PrivateApi")
    private fun powerProfileCapacityMilliAmpHours(context: Context): Double? =
        try {
            val clazz = Class.forName("com.android.internal.os.PowerProfile")
            val profile = clazz.getConstructor(Context::class.java).newInstance(context)
            (clazz.getMethod("getBatteryCapacity").invoke(profile) as? Double)?.takeIf { it > 0 }
        } catch (e: Exception) {
            null
        }
}
