package io.github.plpp555.spectracker.collector

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format

/** 通信機能。接続先の名前（SSID）や IP アドレスなど、位置や個人を特定しうる情報は取得しない。 */
object NetworkCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val appContext = context.applicationContext
        group("現在の接続") {
            row("接続状態") {
                val cm = appContext.getSystemService(ConnectivityManager::class.java)
                val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                if (caps == null) "オフライン" else "オンライン（${transportNames(caps)}）"
            }
            optionalRow("推定リンク速度（下り / 上り）") {
                val cm = appContext.getSystemService(ConnectivityManager::class.java)
                cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }?.let {
                    "${Format.decimal(it.linkDownstreamBandwidthKbps / 1000.0, 0)} Mbps / ${Format.decimal(it.linkUpstreamBandwidthKbps / 1000.0, 0)} Mbps"
                }
            }
            optionalRow("従量課金ネットワーク") {
                val cm = appContext.getSystemService(ConnectivityManager::class.java)
                cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                    ?.let { Format.yesNo(!it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) }
            }
        }
        group("Wi-Fi") {
            val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            row("Wi-Fi") { if (appContext.hasFeature("android.hardware.wifi")) "搭載（${if (wifi.isWifiEnabled) "オン" else "オフ"}）" else "非搭載" }
            row("対応規格") { wifiStandards(wifi) }
            row("5 GHz 帯") { Format.supported(wifi.is5GHzBandSupported) }
            row("6 GHz 帯（Wi-Fi 6E 以降）") {
                if (Build.VERSION.SDK_INT >= 30) Format.supported(wifi.is6GHzBandSupported) else null
            }
            row("Wi-Fi Direct (P2P)") { Format.supported(wifi.isP2pSupported) }
            row("TDLS") { Format.supported(wifi.isTdlsSupported) }
            row("WPA3-SAE") { if (Build.VERSION.SDK_INT >= 30) Format.supported(wifi.isWpa3SaeSupported) else null }
            row("Wi-Fi Aware") { Format.supported(appContext.hasFeature("android.hardware.wifi.aware")) }
            row("Wi-Fi RTT（測距）") { Format.supported(appContext.hasFeature("android.hardware.wifi.rtt")) }
        }
        group("Bluetooth") {
            val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
            row("Bluetooth") { if (adapter == null) "非搭載" else "搭載" }
            if (adapter != null) {
                row("Bluetooth LE") { Format.supported(appContext.hasFeature("android.hardware.bluetooth_le")) }
                row("推定バージョン") { estimateBluetoothVersion(appContext, adapter) }
                row("LE 2M PHY") { Format.supported(adapter.isLe2MPhySupported) }
                row("LE Coded PHY（長距離）") { Format.supported(adapter.isLeCodedPhySupported) }
                row("LE 拡張アドバタイズ") { Format.supported(adapter.isLeExtendedAdvertisingSupported) }
                row("LE 周期アドバタイズ") { Format.supported(adapter.isLePeriodicAdvertisingSupported) }
                row("LE Audio") {
                    if (Build.VERSION.SDK_INT >= 33) {
                        Format.supported(adapter.isLeAudioSupported == BluetoothStatusCodes.FEATURE_SUPPORTED)
                    } else {
                        null
                    }
                }
            }
        }
        group("近距離・測位") {
            row("NFC") { Format.supported(appContext.hasFeature("android.hardware.nfc")) }
            row("NFC カードエミュレーション (HCE)") { Format.supported(appContext.hasFeature("android.hardware.nfc.hce")) }
            row("UWB（超広帯域無線）") { Format.supported(appContext.hasFeature("android.hardware.uwb")) }
            row("赤外線 (IR) 送信") { Format.supported(appContext.hasFeature("android.hardware.consumerir")) }
            row("GPS / GNSS") { Format.supported(appContext.hasFeature("android.hardware.location.gps")) }
            optionalRow("GNSS チップの製造年") {
                if (Build.VERSION.SDK_INT >= 28) {
                    appContext.getSystemService(LocationManager::class.java).gnssYearOfHardware.takeIf { it > 0 }?.toString()
                } else {
                    null
                }
            }
            optionalRow("GNSS ハードウェア名") {
                if (Build.VERSION.SDK_INT >= 28) appContext.getSystemService(LocationManager::class.java).gnssHardwareModelName else null
            }
        }
        if (appContext.hasFeature("android.hardware.telephony")) {
            group("モバイル通信") {
                val tm = appContext.getSystemService(TelephonyManager::class.java)
                row("モデム数") {
                    if (Build.VERSION.SDK_INT >= 30) tm.activeModemCount.toString() else {
                        @Suppress("DEPRECATION")
                        tm.phoneCount.toString()
                    }
                }
                row("方式") {
                    when (tm.phoneType) {
                        TelephonyManager.PHONE_TYPE_GSM -> "GSM 系（3GPP）"
                        TelephonyManager.PHONE_TYPE_CDMA -> "CDMA 系"
                        TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                        else -> "なし"
                    }
                }
                row("SIM の状態") {
                    when (tm.simState) {
                        TelephonyManager.SIM_STATE_READY -> "使用可能"
                        TelephonyManager.SIM_STATE_ABSENT -> "SIM なし"
                        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN 入力待ち"
                        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK 入力待ち"
                        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "ネットワークロック"
                        else -> "不明"
                    }
                }
                optionalRow("ネットワーク事業者") { tm.networkOperatorName }
                optionalRow("SIM の事業者") { tm.simOperatorName }
                optionalRow("SIM の国") { tm.simCountryIso.uppercase() }
                row("SMS") { Format.supported(tm.isSmsCapable) }
                row("音声通話") { Format.supported(tm.isVoiceCapable) }
                row("eSIM") { Format.supported(appContext.hasFeature("android.hardware.telephony.euicc")) }
            }
        }
    }

    private fun transportNames(caps: NetworkCapabilities): String {
        val names = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("モバイル回線")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("有線 LAN")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        }
        return names.joinToString(" + ").ifEmpty { "その他" }
    }

    private fun wifiStandards(wifi: WifiManager): String? {
        if (Build.VERSION.SDK_INT < 30) return null
        val standards = buildList {
            if (Build.VERSION.SDK_INT >= 33 && wifi.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11BE)) add("Wi-Fi 7 (802.11be)")
            if (wifi.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AX)) add("Wi-Fi 6 (802.11ax)")
            if (wifi.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AC)) add("Wi-Fi 5 (802.11ac)")
            if (wifi.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11N)) add("Wi-Fi 4 (802.11n)")
        }
        return standards.joinToString(" / ").ifEmpty { "802.11n 未満" }
    }

    /** Bluetooth のバージョンは直接取得できないため、対応機能から推定する。 */
    private fun estimateBluetoothVersion(context: Context, adapter: BluetoothAdapter): String =
        when {
            adapter.isLeExtendedAdvertisingSupported || adapter.isLe2MPhySupported || adapter.isLeCodedPhySupported -> "5.0 以上"
            context.hasFeature("android.hardware.bluetooth_le") -> "4.0 以上"
            else -> "3.0 以下"
        }
}
