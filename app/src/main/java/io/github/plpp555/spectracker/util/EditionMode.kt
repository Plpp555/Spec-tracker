package io.github.plpp555.spectracker.util

/**
 * 演出の表示モード。通常は [AUTO]（実機の判定どおり）。
 * それ以外はデバッグ版の切り替えスイッチ専用で、公開版では常に [AUTO] になる。
 */
enum class EditionMode(val label: String) {
    AUTO("自動（実機の判定どおり）"),
    NORMAL("通常"),
    ULTRA("Ultra 版"),
    EDITION("特装版"),
    ULTRA_EDITION("Ultra 版 + 特装版");

    /** [real] は実機の判定結果、[sampleEditionName] は特装版を模擬するときにポップアップへ出す端末名。 */
    fun resolve(real: EditionInfo, sampleEditionName: String): EditionInfo = when (this) {
        AUTO -> real
        NORMAL -> EditionInfo.NONE
        ULTRA -> EditionInfo(isUltra = true, editionName = null)
        EDITION -> EditionInfo(isUltra = false, editionName = sampleEditionName)
        ULTRA_EDITION -> EditionInfo(isUltra = true, editionName = sampleEditionName)
    }
}
