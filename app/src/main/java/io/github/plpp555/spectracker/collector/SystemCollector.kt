package io.github.plpp555.spectracker.collector

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.webkit.WebView
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format
import io.github.plpp555.spectracker.util.SystemProps
import io.github.plpp555.spectracker.util.readFileOrNull
import java.util.Locale
import java.util.TimeZone

object SystemCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        group("Android") {
            row("バージョン") { "Android ${Build.VERSION.RELEASE}" }
            row("API レベル") { Build.VERSION.SDK_INT.toString() }
            row("コードネーム") { Build.VERSION.CODENAME.takeUnless { it == "REL" } ?: "正式リリース (REL)" }
            row("セキュリティパッチ") { Build.VERSION.SECURITY_PATCH }
            optionalRow("ベンダーのセキュリティパッチ") { SystemProps["ro.vendor.build.security_patch"] }
            row("ビルド番号 (incremental)") { Build.VERSION.INCREMENTAL }
            optionalRow("初回出荷時の API レベル") {
                SystemProps["ro.product.first_api_level"]
            }
            optionalRow("メディアパフォーマンスクラス") {
                if (Build.VERSION.SDK_INT >= 31) Build.VERSION.MEDIA_PERFORMANCE_CLASS.takeIf { it > 0 }?.toString() else null
            }
            optionalRow("プレビュー版 SDK") { Build.VERSION.PREVIEW_SDK_INT.takeIf { it > 0 }?.toString() }
        }
        group("カーネル / ランタイム") {
            row("カーネル") { System.getProperty("os.version") }
            row("カーネルのアーキテクチャ") { System.getProperty("os.arch") }
            optionalRow("カーネルのビルド情報") { readFileOrNull("/proc/version") }
            row("Java 仮想マシン") {
                "${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}"
            }
            optionalRow("VM ベンダー") { System.getProperty("java.vm.vendor") }
        }
        group("セキュリティ") {
            row("画面ロック") {
                val keyguard = context.getSystemService(KeyguardManager::class.java)
                if (keyguard.isDeviceSecure) "設定済み" else "未設定"
            }
            row("ストレージ暗号化") {
                val dpm = context.getSystemService(DevicePolicyManager::class.java)
                when (dpm.storageEncryptionStatus) {
                    DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "非対応"
                    DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "未暗号化"
                    DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "暗号化中"
                    DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "暗号化済み（ファイルベース暗号化）"
                    else -> "暗号化済み"
                }
            }
            optionalRow("Verified Boot の状態") {
                when (SystemProps["ro.boot.verifiedbootstate"]) {
                    "green" -> "検証済み (green)"
                    "yellow" -> "独自の鍵で検証 (yellow)"
                    "orange" -> "ブートローダーがアンロック (orange)"
                    "red" -> "検証失敗 (red)"
                    else -> null
                }
            }
            optionalRow("ブートローダー") {
                when {
                    SystemProps["ro.boot.flash.locked"] == "1" || SystemProps["ro.boot.vbmeta.device_state"] == "locked" -> "ロック済み"
                    SystemProps["ro.boot.flash.locked"] == "0" || SystemProps["ro.boot.vbmeta.device_state"] == "unlocked" -> "アンロック済み"
                    else -> null
                }
            }
            optionalRow("SELinux") {
                when (readFileOrNull("/sys/fs/selinux/enforce")) {
                    "1" -> "Enforcing（強制）"
                    "0" -> "Permissive（許容）"
                    else -> null
                }
            }
            row("ハードウェアバックの鍵ストア") {
                val version = context.packageManager.featureVersion("android.hardware.hardware_keystore")
                if (version != null) "対応 (v$version)" else Format.supported(false)
            }
            row("StrongBox") { Format.supported(context.hasFeature("android.hardware.strongbox_keystore")) }
            row("指紋認証ハードウェア") { Format.supported(context.hasFeature("android.hardware.fingerprint")) }
            row("顔認証ハードウェア") { Format.supported(context.hasFeature("android.hardware.biometrics.face")) }
            row("虹彩認証ハードウェア") { Format.supported(context.hasFeature("android.hardware.biometrics.iris")) }
        }
        group("システム構成") {
            optionalRow("Project Treble") { SystemProps["ro.treble.enabled"]?.let { Format.supported(it == "true") } }
            optionalRow("A/B パーティション") { SystemProps["ro.build.ab_update"]?.let { Format.supported(it == "true") } }
            optionalRow("Virtual A/B") { SystemProps["ro.virtual_ab.enabled"]?.let { Format.supported(it == "true") } }
            optionalRow("動的パーティション") { SystemProps["ro.boot.dynamic_partitions"]?.let { Format.supported(it == "true") } }
        }
        group("ソフトウェア") {
            row("WebView") {
                val pkg = WebView.getCurrentWebViewPackage()
                if (pkg == null) null else "${pkg.packageName} ${pkg.versionName}"
            }
            optionalRow("Google Play 開発者サービス") {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName
            }
            row("言語 / 地域") { Locale.getDefault().toLanguageTag() }
            row("タイムゾーン") { TimeZone.getDefault().id }
            row("起動してからの時間") { Format.duration(SystemClock.elapsedRealtime()) }
        }
    }
}
