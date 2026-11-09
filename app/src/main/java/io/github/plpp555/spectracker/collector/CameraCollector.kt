package io.github.plpp555.spectracker.collector

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.SpecItem
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.model.runCatchingValue
import io.github.plpp555.spectracker.util.Format
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** カメラの性能。Camera2 の特性情報の参照にはカメラの権限は不要（撮影はしない）。 */
object CameraCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val manager = context.getSystemService(CameraManager::class.java)
        val ids = manager.cameraIdList
        group("概要") {
            row("カメラ数", "${ids.size} 台")
        }
        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            group("カメラ $id（${facingText(characteristics)}）") {
                describe(characteristics).forEach { row(it.label, it.value) }
            }
            if (Build.VERSION.SDK_INT >= 28) {
                // 論理マルチカメラ（広角・望遠などをまとめた仮想カメラ）の実体
                for (physicalId in characteristics.physicalCameraIds) {
                    val physical = try {
                        manager.getCameraCharacteristics(physicalId)
                    } catch (e: Exception) {
                        null
                    } ?: continue
                    group("　└ 物理カメラ $physicalId（カメラ $id の構成）") {
                        describe(physical).forEach { row(it.label, it.value) }
                    }
                }
            }
        }
    }

    fun count(context: Context): Int? =
        try {
            context.getSystemService(CameraManager::class.java).cameraIdList.size
        } catch (e: Exception) {
            null
        }

    private fun describe(c: CameraCharacteristics): List<SpecItem> {
        val items = mutableListOf<SpecItem>()
        fun add(label: String, block: () -> String?) {
            runCatchingValue(block)?.let { items += SpecItem(label, it) }
        }

        val pixelArray = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val physicalSize = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val focalLengths = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

        add("画素数") {
            pixelArray?.let { "${it.width} × ${it.height} px（${Format.decimal(it.width.toLong() * it.height / 1_000_000.0, 1)} MP）" }
        }
        add("センサーサイズ") { physicalSize?.let { "${Format.decimal(it.width.toDouble())} × ${Format.decimal(it.height.toDouble())} mm" } }
        add("画素ピッチ（推定）") {
            if (pixelArray != null && physicalSize != null) "${Format.decimal(physicalSize.width * 1000.0 / pixelArray.width)} µm" else null
        }
        add("焦点距離") { focalLengths?.joinToString(", ") { "${Format.decimal(it.toDouble(), 1)} mm" } }
        add("35mm 換算焦点距離") {
            if (physicalSize != null && focalLengths != null) {
                val diagonal = sqrt((physicalSize.width * physicalSize.width + physicalSize.height * physicalSize.height).toDouble())
                focalLengths.joinToString(", ") { "${(it * 43.267 / diagonal).roundToInt()} mm" }
            } else {
                null
            }
        }
        add("水平画角（推定）") {
            if (physicalSize != null && focalLengths != null) {
                focalLengths.joinToString(", ") {
                    "${(2 * Math.toDegrees(atan(physicalSize.width / (2.0 * it)))).roundToInt()}°"
                }
            } else {
                null
            }
        }
        add("絞り値 (F値)") {
            c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.joinToString(", ") { "f/${Format.decimal(it.toDouble(), 1)}" }
        }
        add("オートフォーカス") {
            val modes = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            val minFocus = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            when {
                modes == null -> null
                minFocus != null && minFocus > 0f -> "対応（最短撮影距離 約 ${Format.decimal(100.0 / minFocus, 0)} cm）"
                modes.size > 1 -> "対応"
                else -> "非対応（固定焦点）"
            }
        }
        add("光学手ぶれ補正 (OIS)") {
            val modes = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            Format.supported(modes?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true)
        }
        add("動画の手ぶれ補正") {
            val modes = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            Format.supported(modes?.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF } == true)
        }
        add("フラッシュ") { c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)?.let { Format.supported(it) } }
        add("ズーム倍率") {
            val zoomRange = if (Build.VERSION.SDK_INT >= 30) c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) else null
            if (zoomRange != null) {
                "${Format.decimal(zoomRange.lower.toDouble(), 1)}x 〜 ${Format.decimal(zoomRange.upper.toDouble(), 1)}x"
            } else {
                c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { "最大 ${Format.decimal(it.toDouble(), 1)}x（デジタル）" }
            }
        }
        add("ISO 感度") { c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { "${it.lower} 〜 ${it.upper}" } }
        add("露出時間") {
            c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let {
                "1/${(1e9 / it.lower).roundToInt()} 秒 〜 ${Format.decimal(it.upper / 1e9, 1)} 秒"
            }
        }
        val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        add("最大の静止画サイズ") {
            map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
                ?.let { "${it.width} × ${it.height} px" }
        }
        add("最大の動画サイズ") {
            map?.getOutputSizes(MediaRecorder::class.java)?.maxByOrNull { it.width.toLong() * it.height }
                ?.let { "${it.width} × ${it.height} px" }
        }
        add("ハードウェアレベル") {
            when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY（旧 API 互換）"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> null
            }
        }
        add("対応機能") {
            c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.toList()
                ?.mapNotNull { capabilityName(it) }
                ?.joinToString(", ")
        }
        add("センサーの向き") { c.get(CameraCharacteristics.SENSOR_ORIENTATION)?.let { "${it}°" } }
        return items
    }

    private fun facingText(c: CameraCharacteristics): String =
        when (c.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> "前面"
            CameraCharacteristics.LENS_FACING_BACK -> "背面"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "外部"
            else -> "不明"
        }

    private fun capabilityName(capability: Int): String? = when (capability) {
        1 -> "マニュアルセンサー"
        2 -> "マニュアル画像処理"
        3 -> "RAW 撮影"
        4 -> "非圧縮リプロセス"
        5 -> "センサー設定の読み取り"
        6 -> "バースト撮影"
        7 -> "YUV リプロセス"
        8 -> "深度出力"
        9 -> "高速動画"
        10 -> "モーショントラッキング"
        11 -> "論理マルチカメラ"
        12 -> "モノクロ"
        13 -> "セキュア画像"
        14 -> "システムカメラ"
        15 -> "オフライン処理"
        16 -> "超高解像度センサー"
        17 -> "リモザイクリプロセス"
        18 -> "10 ビット HDR"
        19 -> "ストリーム用途指定"
        20 -> "色空間プロファイル"
        else -> null
    }
}
