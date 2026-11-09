package io.github.plpp555.spectracker.collector

import android.content.Context
import io.github.plpp555.spectracker.model.SpecCategory
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.SpecItem
import io.github.plpp555.spectracker.model.UNAVAILABLE

/** 画面に表示するカテゴリ（タブ）の一覧。 */
object SpecCatalog {
    val categories: List<SpecCategory> = listOf(
        category("overview", "概要", 0, OverviewCollector::collect),
        category("device", "デバイス", 0, DeviceCollector::collect),
        category("system", "システム", 5_000, SystemCollector::collect),
        category("cpu", "CPU", 1_000, CpuCollector::collect),
        category("gpu", "GPU", 0, GpuCollector::collect),
        category("memory", "メモリ", 2_000, MemoryCollector::collect),
        category("storage", "ストレージ", 0, StorageCollector::collect),
        category("display", "ディスプレイ", 0, DisplayCollector::collect),
        category("battery", "バッテリー", 1_000, BatteryCollector::collect),
        category("thermal", "温度", 2_000, ThermalCollector::collect),
        category("camera", "カメラ", 0, CameraCollector::collect),
        category("sensor", "センサー", 0, SensorCollector::collect),
        category("network", "ネットワーク", 0, NetworkCollector::collect),
        category("hardware", "その他HW", 0, MiscHardwareCollector::collect),
        category("features", "機能一覧", 0, FeaturesCollector::collect),
    )

    /** どのカテゴリでも、取得処理の失敗でアプリが落ちないよう最後の砦として例外を受ける。 */
    private fun category(
        id: String,
        title: String,
        refreshMillis: Long,
        collector: (Context) -> List<SpecGroup>,
    ) = SpecCategory(id, title, refreshMillis) { context ->
        try {
            collector(context)
        } catch (e: Exception) {
            listOf(SpecGroup(null, listOf(SpecItem("状態", "$UNAVAILABLE（${e.javaClass.simpleName}）"))))
        }
    }
}
