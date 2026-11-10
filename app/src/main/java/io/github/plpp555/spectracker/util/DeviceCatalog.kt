package io.github.plpp555.spectracker.util

/**
 * 型番 → 販売名の対応表。
 *
 * 端末が販売名（ro.product.marketname）を名乗らない場合に、型番（Build.MODEL）から販売名を補うために使う。
 * 補った販売名は、Ultra / 特装版の判定にそのまま使われる。
 *
 * 追加するときの注意:
 *  - 公式情報などで型番を確認できたものだけを載せる（誤ると無関係な端末に演出が出る）。
 *  - 別の機種まで巻き込む恐れがある型番は prefix = false（完全一致）にする。
 *    例: Xiaomi の M2102K1G（Mi 11 Ultra）は、M2102K1AC（Mi 11 Pro）と先頭が同じ。
 *  - 特装版は型番がベース機種と同じことが多い。特装版だけの型番が分かったときは、販売名を「… Edition」にして追加する。
 */
object DeviceCatalog {
    private class Entry(val model: String, val prefix: Boolean, val marketName: String)

    private fun prefix(model: String, marketName: String) = Entry(model, true, marketName)
    private fun exact(model: String, marketName: String) = Entry(model, false, marketName)

    private val entries = listOf(
        // ---- Samsung Galaxy（型番の先頭が同じなら地域・キャリア違いも同じ機種。例: SM-S928B / SM-S928U / SM-S928N）----
        prefix("SM-G988", "Galaxy S20 Ultra"),
        prefix("SM-G998", "Galaxy S21 Ultra"),
        prefix("SM-S908", "Galaxy S22 Ultra"),
        prefix("SM-S918", "Galaxy S23 Ultra"),
        prefix("SM-S928", "Galaxy S24 Ultra"),
        prefix("SM-S938", "Galaxy S25 Ultra"),
        // ---- Samsung Galaxy 日本向け（docomo: SC-、au: SCG）----
        exact("SC-52C", "Galaxy S22 Ultra"),
        exact("SCG14", "Galaxy S22 Ultra"),
        exact("SC-52D", "Galaxy S23 Ultra"),
        exact("SCG20", "Galaxy S23 Ultra"),
        exact("SC-52E", "Galaxy S24 Ultra"),
        exact("SCG26", "Galaxy S24 Ultra"),
        // ---- Xiaomi（完全一致のみ）----
        exact("M2007J1SC", "Xiaomi Mi 10 Ultra"),
        exact("M2102K1G", "Xiaomi Mi 11 Ultra"),
        exact("M2102K1C", "Xiaomi Mi 11 Ultra"),
        exact("2304FPN6DC", "Xiaomi 13 Ultra"),
        exact("2304FPN6DG", "Xiaomi 13 Ultra"),
        exact("24030PN60G", "Xiaomi 14 Ultra"),
        exact("25010PN30G", "Xiaomi 15 Ultra"),
    )

    /** 型番から販売名を引く。載っていなければ null。大文字小文字は区別しない。 */
    fun marketNameFor(model: String?): String? {
        val key = model?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        return entries
            .filter { if (it.prefix) key.startsWith(it.model) else key == it.model }
            .maxByOrNull { it.model.length }
            ?.marketName
    }
}
