package io.github.plpp555.spectracker.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format

object SensorCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val sensors = sensors(context)
        group("概要") {
            row("センサー数", "${sensors.size} 個")
        }
        for (sensor in sensors) {
            group(sensor.name) {
                row("種類") { sensor.stringType.removePrefix("android.sensor.") }
                row("ベンダー") { sensor.vendor }
                row("バージョン") { sensor.version.toString() }
                val unit = unitOf(sensor.type)
                row("測定範囲（最大）") { "${Format.decimal(sensor.maximumRange.toDouble())}${unit?.let { " $it" }.orEmpty()}" }
                row("分解能") { "${Format.decimal(sensor.resolution.toDouble(), 4)}${unit?.let { " $it" }.orEmpty()}" }
                row("最大サンプリング周波数") {
                    when {
                        sensor.minDelay > 0 -> "${Format.decimal(1_000_000.0 / sensor.minDelay, 0)} Hz"
                        sensor.minDelay == 0 -> "変化時のみ通知 / 可変"
                        else -> "1 回のみ（ワンショット）"
                    }
                }
                row("通知方式") {
                    when (sensor.reportingMode) {
                        Sensor.REPORTING_MODE_CONTINUOUS -> "連続"
                        Sensor.REPORTING_MODE_ON_CHANGE -> "変化時"
                        Sensor.REPORTING_MODE_ONE_SHOT -> "ワンショット"
                        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "特殊トリガー"
                        else -> null
                    }
                }
                row("ウェイクアップ") { Format.yesNo(sensor.isWakeUpSensor) }
                row("動的センサー") { Format.yesNo(sensor.isDynamicSensor) }
                optionalRow("FIFO の最大イベント数") { sensor.fifoMaxEventCount.takeIf { it > 0 }?.toString() }
            }
        }
    }

    fun count(context: Context): Int? =
        try {
            sensors(context).size
        } catch (e: Exception) {
            null
        }

    private fun sensors(context: Context): List<Sensor> =
        context.getSystemService(SensorManager::class.java)
            .getSensorList(Sensor.TYPE_ALL)
            .sortedWith(compareBy({ it.type }, { it.name }))

    /** センサーの種類ごとの測定単位。温度は ℃。 */
    private fun unitOf(type: Int): String? = when (type) {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
        Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GRAVITY -> "m/s²"
        Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "rad/s"
        Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "µT"
        Sensor.TYPE_LIGHT -> "lx"
        Sensor.TYPE_PROXIMITY -> "cm"
        Sensor.TYPE_PRESSURE -> "hPa"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "℃"
        @Suppress("DEPRECATION") Sensor.TYPE_TEMPERATURE -> "℃"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
        Sensor.TYPE_HEART_RATE -> "bpm"
        else -> null
    }
}
