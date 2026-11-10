package io.github.plpp555.spectracker.util

import android.os.Build

/**
 * 端末の販売名から決まる演出の種類。
 *
 * @param isUltra 販売名に単語として "Ultra" を含む（例: Galaxy S24 Ultra）
 * @param editionName 特装版（○○ Edition）なら、ポップアップに出す端末名。地域エディションや該当なしは null
 */
data class EditionInfo(val isUltra: Boolean, val editionName: String?) {
    companion object {
        val NONE = EditionInfo(isUltra = false, editionName = null)
    }
}

object DeviceEdition {
    /** 「Japan Edition」のような地域エディションは特装版として扱わない。 */
    private val regionWords = setOf(
        "japan", "japanese", "jp", "global", "international", "china", "chinese", "cn",
        "india", "indian", "in", "eu", "europe", "european", "us", "usa", "uk", "korea", "korean", "kr",
        "taiwan", "tw", "kong", "asia", "apac", "emea", "latin", "america", "brazil", "russia", "russian",
        "australia", "canada", "mexico", "middle", "east", "africa", "oceania",
    )

    private val ultra = Regex("""(?i)(^|[^a-z])ultra([^a-z]|$)""")
    private val edition = Regex("""(?i)([a-z0-9]+)[\s\-_]*edition(?![a-z])""")

    /**
     * @param names 販売名の候補（優先度の高い順。型番しか無い端末では型番）
     * @param manufacturer 端末名の先頭に補うメーカー名
     */
    fun detect(names: List<String>, manufacturer: String): EditionInfo {
        val candidates = names.map { it.trim() }.filter { it.isNotEmpty() }
        val isUltra = candidates.any { ultra.containsMatchIn(it) }
        val special = candidates.firstOrNull { isSpecialEdition(it) }
        return EditionInfo(isUltra, special?.let { displayName(it, manufacturer) })
    }

    private fun isSpecialEdition(name: String): Boolean =
        edition.findAll(name).any { it.groupValues[1].lowercase() !in regionWords }

    private fun displayName(name: String, manufacturer: String): String {
        val brand = manufacturer.trim()
        if (brand.isEmpty() || name.contains(brand, ignoreCase = true)) return name
        return "${brand.replaceFirstChar { it.uppercase() }} $name"
    }

    /** この端末の判定結果。getprop を実行するため、メインスレッド以外で呼ぶこと。 */
    fun current(): EditionInfo {
        val names = listOfNotNull(
            SystemProps["ro.product.marketname"],
            SystemProps["ro.product.odm.marketname"],
            SystemProps["ro.product.vendor.marketname"],
            SystemProps["ro.product.system.marketname"],
            Build.MODEL,
        )
        return detect(names, Build.MANUFACTURER)
    }
}
