package io.github.plpp555.spectracker.util

import java.io.File

/** ファイルを読み取り、読めない（存在しない・SELinux で拒否など）場合は null を返す。 */
internal fun readFileOrNull(path: String): String? =
    try {
        File(path).bufferedReader().use { it.readText() }.trim().takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

internal fun readLongOrNull(path: String): Long? = readFileOrNull(path)?.toLongOrNull()
