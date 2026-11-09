package io.github.plpp555.spectracker.util

/** `getprop` の出力を 1 回だけ読み込んで保持する。表示する項目は呼び出し側で限定する（識別子系は表示しない）。 */
object SystemProps {
    private val entry = Regex("""^\[(.*?)]: \[(.*)]$""")

    private val props: Map<String, String> by lazy { readAll() }

    operator fun get(key: String): String? = props[key]?.takeIf { it.isNotBlank() }

    private fun readAll(): Map<String, String> =
        try {
            val process = ProcessBuilder("getprop").redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            buildMap {
                for (line in text.lineSequence()) {
                    entry.matchEntire(line.trim())?.let { put(it.groupValues[1], it.groupValues[2]) }
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
}
