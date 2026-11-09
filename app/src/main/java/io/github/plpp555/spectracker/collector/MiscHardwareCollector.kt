package io.github.plpp555.spectracker.collector

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Vibrator
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format

object MiscHardwareCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        group("オーディオ") {
            val audio = context.getSystemService(AudioManager::class.java)
            row("出力サンプルレート（推奨）") { audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.let { "$it Hz" } }
            row("出力バッファサイズ（推奨）") { audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.let { "$it フレーム" } }
            row("低遅延オーディオ") { Format.supported(context.hasFeature("android.hardware.audio.low_latency")) }
            row("プロ向けオーディオ") { Format.supported(context.hasFeature("android.hardware.audio.pro")) }
            row("マイク") { Format.supported(context.hasFeature("android.hardware.microphone")) }
            row("3.5mm ヘッドホン端子の出力を検出") {
                Format.yesNo(
                    audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    },
                )
            }
            row("出力デバイス") {
                audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .map { outputTypeName(it.type) }
                    .distinct()
                    .joinToString(", ")
            }
        }
        group("振動") {
            val vibrator = context.getSystemService(Vibrator::class.java)
            row("バイブレーター") { Format.supported(vibrator.hasVibrator()) }
            row("振動の強さ調整") { Format.supported(vibrator.hasAmplitudeControl()) }
        }
        group("タッチ・入力") {
            row("タッチスクリーン") { Format.supported(context.hasFeature("android.hardware.touchscreen")) }
            row("マルチタッチ") {
                when {
                    context.hasFeature("android.hardware.touchscreen.multitouch.jazzhand") -> "5 点以上"
                    context.hasFeature("android.hardware.touchscreen.multitouch.distinct") -> "2 点以上（個別認識）"
                    context.hasFeature("android.hardware.touchscreen.multitouch") -> "2 点"
                    else -> "非対応"
                }
            }
            row("スタイラス") { Format.supported(context.hasFeature("android.hardware.touchscreen.pen")) }
        }
        group("接続・その他") {
            row("USB ホスト (OTG)") { Format.supported(context.hasFeature("android.hardware.usb.host")) }
            row("USB アクセサリ") { Format.supported(context.hasFeature("android.hardware.usb.accessory")) }
            row("HDMI 出力") { Format.supported(context.hasFeature("android.hardware.type.television")) }
            row("ライブ壁紙") { Format.supported(context.hasFeature("android.software.live_wallpaper")) }
            row("ピクチャインピクチャ") { Format.supported(context.hasFeature("android.software.picture_in_picture")) }
            row("折りたたみ（ヒンジ角センサー）") { Format.supported(context.hasFeature("android.hardware.sensor.hinge_angle")) }
        }
    }

    private fun outputTypeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "内蔵レシーバー"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "内蔵スピーカー"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有線ヘッドセット"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "有線ヘッドホン"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (SCO)"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (A2DP)"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB オーディオ"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB ヘッドセット"
        AudioDeviceInfo.TYPE_TELEPHONY -> "通話"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "内蔵スピーカー（保護）"
        else -> "その他 (type $type)"
    }
}
