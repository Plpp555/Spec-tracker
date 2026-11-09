package io.github.plpp555.spectracker.collector

import android.content.Context
import android.os.Build
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.SystemProps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        group("端末") {
            row("メーカー") { Build.MANUFACTURER }
            row("ブランド") { Build.BRAND }
            row("モデル") { Build.MODEL }
            optionalRow("市場名") { SystemProps["ro.product.marketname"] ?: SystemProps["ro.product.odm.marketname"] }
            row("製品名 (product)") { Build.PRODUCT }
            row("デバイス名 (device)") { Build.DEVICE }
            row("ボード") { Build.BOARD }
            row("ハードウェア") { Build.HARDWARE }
            optionalRow("端末の種類") { SystemProps["ro.build.characteristics"] }
            optionalRow("SKU") { if (Build.VERSION.SDK_INT >= 31) Build.SKU.takeUnless { it == Build.UNKNOWN } else null }
            optionalRow("ブートローダー") { Build.BOOTLOADER.takeUnless { it == Build.UNKNOWN } }
            optionalRow("ベースバンド（モデム）") { Build.getRadioVersion() }
        }
        group("ビルド情報") {
            row("ビルド ID") { Build.ID }
            row("ビルド表示名") { Build.DISPLAY }
            row("ビルド種別") { Build.TYPE }
            row("タグ") { Build.TAGS }
            row("ビルド日時") {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ROOT).format(Date(Build.TIME))
            }
            row("ビルドしたユーザー / ホスト") { "${Build.USER} @ ${Build.HOST}" }
            row("フィンガープリント") { Build.FINGERPRINT }
        }
    }
}
