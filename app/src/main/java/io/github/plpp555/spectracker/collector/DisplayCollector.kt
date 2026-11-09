package io.github.plpp555.spectracker.collector

import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format
import kotlin.math.sqrt

object DisplayCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        group("内蔵ディスプレイ") {
            row("名称") { display.name }
            row("解像度") { "${metrics.widthPixels} × ${metrics.heightPixels} px" }
            row("アスペクト比") { aspectRatio(metrics.widthPixels, metrics.heightPixels) }
            row("画面サイズ（推定）") {
                val widthInch = metrics.widthPixels / metrics.xdpi
                val heightInch = metrics.heightPixels / metrics.ydpi
                "${Format.decimal(sqrt((widthInch * widthInch + heightInch * heightInch).toDouble()), 1)} インチ"
            }
            row("画素密度") { "${metrics.densityDpi} dpi（${densityBucket(metrics.densityDpi)}）" }
            row("実測 dpi（横 / 縦）") { "${Format.decimal(metrics.xdpi.toDouble(), 1)} / ${Format.decimal(metrics.ydpi.toDouble(), 1)}" }
            row("密度スケール") { Format.decimal(metrics.density.toDouble()) }
            row("文字サイズ倍率") { Format.decimal(context.resources.configuration.fontScale.toDouble()) }
        }
        group("リフレッシュレート") {
            row("現在") { "${Format.decimal(display.refreshRate.toDouble(), 1)} Hz" }
            row("対応モード") {
                display.supportedModes
                    .map { "${it.physicalWidth} × ${it.physicalHeight} @ ${Format.decimal(it.refreshRate.toDouble(), 1)} Hz" }
                    .distinct()
                    .joinToString("\n")
            }
        }
        group("色・HDR") {
            row("広色域 (Wide Color Gamut)") { Format.supported(display.isWideColorGamut) }
            row("HDR") {
                @Suppress("DEPRECATION")
                val caps = display.hdrCapabilities
                val types = caps?.supportedHdrTypes?.toList()?.mapNotNull { hdrTypeName(it) }.orEmpty()
                if (types.isEmpty()) "非対応" else types.joinToString(", ")
            }
            optionalRow("最大輝度（HDR 時の目安）") {
                @Suppress("DEPRECATION")
                val caps = display.hdrCapabilities
                caps?.desiredMaxLuminance?.takeIf { it > 0 }?.let { "${Format.decimal(it.toDouble(), 0)} cd/m²" }
            }
        }
        group("状態") {
            row("画面の向き") {
                when (display.rotation) {
                    android.view.Surface.ROTATION_0 -> "縦（0°）"
                    android.view.Surface.ROTATION_90 -> "横（90°）"
                    android.view.Surface.ROTATION_180 -> "縦・逆さ（180°）"
                    android.view.Surface.ROTATION_270 -> "横・逆向き（270°）"
                    else -> null
                }
            }
            row("ダークテーマ") {
                val night = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                Format.yesNo(night == android.content.res.Configuration.UI_MODE_NIGHT_YES)
            }
            optionalRow("明るさ（0〜255）") {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS).toString()
            }
            optionalRow("明るさの自動調整") {
                val mode = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                Format.yesNo(mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            }
        }
        val all = displayManager.displays
        if (all.size > 1) {
            group("接続中のディスプレイ") {
                for (d in all) {
                    row("ディスプレイ ${d.displayId}") {
                        val m = DisplayMetrics()
                        @Suppress("DEPRECATION")
                        d.getRealMetrics(m)
                        "${d.name}｜${m.widthPixels} × ${m.heightPixels} px"
                    }
                }
            }
        }
    }

    fun summary(context: Context): String? {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return "${metrics.widthPixels} × ${metrics.heightPixels}｜${Format.decimal(display.refreshRate.toDouble(), 0)} Hz｜${metrics.densityDpi} dpi"
    }

    private fun aspectRatio(width: Int, height: Int): String {
        val long = maxOf(width, height).toDouble()
        val short = minOf(width, height).toDouble()
        return "${Format.decimal(long / short * 9, 1)} : 9"
    }

    private fun densityBucket(dpi: Int): String = when {
        dpi <= 120 -> "ldpi"
        dpi <= 160 -> "mdpi"
        dpi <= 213 -> "tvdpi"
        dpi <= 240 -> "hdpi"
        dpi <= 320 -> "xhdpi"
        dpi <= 480 -> "xxhdpi"
        else -> "xxxhdpi"
    }

    private fun hdrTypeName(type: Int): String? = when (type) {
        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
        else -> null
    }
}
