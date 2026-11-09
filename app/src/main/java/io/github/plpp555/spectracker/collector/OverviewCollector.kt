package io.github.plpp555.spectracker.collector

import android.content.Context
import android.os.Build
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format

/** 主要な項目だけをまとめた概要。詳細は各タブで確認できる。 */
object OverviewCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        group("主なスペック") {
            row("端末") { "${Build.MANUFACTURER} ${Build.MODEL}" }
            row("OS") { "Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）" }
            row("CPU") { CpuCollector.summary(context) }
            row("GPU") { GpuCollector.rendererOrNull() }
            row("RAM") { MemoryCollector.totalText(context) }
            row("ストレージ") { StorageCollector.summary(context) }
            row("ディスプレイ") { DisplayCollector.summary(context) }
            row("バッテリー") { BatteryCollector.summary(context) }
            row("カメラ") { CameraCollector.count(context)?.let { "$it 台" } }
            row("センサー") { SensorCollector.count(context)?.let { "$it 個" } }
            row("対応ネットワーク") {
                val parts = buildList {
                    if (context.hasFeature("android.hardware.wifi")) add("Wi-Fi")
                    if (context.hasFeature("android.hardware.bluetooth")) add("Bluetooth")
                    if (context.hasFeature("android.hardware.nfc")) add("NFC")
                    if (context.hasFeature("android.hardware.telephony")) add("モバイル通信")
                }
                parts.joinToString(" / ").ifEmpty { null }
            }
        }
        group("このアプリについて") {
            row("バージョン") {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
            row("単位の表記") { "バッテリーは W / Wh、温度は ℃ で統一" }
            row("データの取り扱い") { "取得した情報は端末内でのみ表示し、外部へ送信しません（通信権限なし）" }
        }
    }
}
