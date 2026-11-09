package io.github.plpp555.spectracker.model

import android.content.Context

/** 画面に表示する 1 行（項目名と値）。 */
data class SpecItem(val label: String, val value: String)

/** 見出し付きの項目のまとまり。 */
data class SpecGroup(val title: String?, val items: List<SpecItem>)

/**
 * 1 つのカテゴリ（タブ）。
 *
 * @param refreshMillis 0 なら表示時に 1 回だけ取得、0 より大きければその間隔で再取得する（ライブ更新）。
 * @param collect 重い処理・ファイル読み取りを含むため、必ずバックグラウンドスレッドで呼ぶこと。
 */
class SpecCategory(
    val id: String,
    val title: String,
    val refreshMillis: Long = 0L,
    val collect: (Context) -> List<SpecGroup>,
)

const val UNAVAILABLE = "取得不可"

/** 値の取得に失敗（例外 / null / 空）した場合は null を返す。1 項目の失敗で全体を落とさないための共通処理。 */
internal inline fun runCatchingValue(block: () -> String?): String? =
    try {
        block()?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    } catch (e: LinkageError) {
        // 非公開 API（リフレクション）が端末に存在しない場合など
        null
    }

class GroupListBuilder {
    private val groups = mutableListOf<SpecGroup>()

    fun group(title: String? = null, block: GroupBuilder.() -> Unit) {
        val items = GroupBuilder().apply(block).items
        if (items.isNotEmpty()) groups += SpecGroup(title, items)
    }

    internal fun build(): List<SpecGroup> = groups.toList()
}

class GroupBuilder {
    internal val items = mutableListOf<SpecItem>()

    /** 固定の値を追加する。 */
    fun row(label: String, value: String) {
        items += SpecItem(label, value)
    }

    /** 値を取得して追加する。取得できなければ「取得不可」と表示する。 */
    fun row(label: String, block: () -> String?) {
        items += SpecItem(label, runCatchingValue(block) ?: UNAVAILABLE)
    }

    /** 値を取得して追加する。取得できなければ行ごと省略する。 */
    fun optionalRow(label: String, block: () -> String?) {
        runCatchingValue(block)?.let { items += SpecItem(label, it) }
    }
}

fun buildGroups(block: GroupListBuilder.() -> Unit): List<SpecGroup> =
    GroupListBuilder().apply(block).build()

/** 全カテゴリを共有用のプレーンテキストにする。 */
fun List<Pair<String, List<SpecGroup>>>.toPlainText(header: String): String = buildString {
    appendLine(header)
    for ((title, groups) in this@toPlainText) {
        appendLine()
        appendLine("■ $title")
        for (group in groups) {
            group.title?.let { appendLine("[$it]") }
            for (item in group.items) appendLine("${item.label}: ${item.value}")
        }
    }
}
