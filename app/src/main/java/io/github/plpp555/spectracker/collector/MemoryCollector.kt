package io.github.plpp555.spectracker.collector

import android.app.ActivityManager
import android.content.Context
import android.system.Os
import android.system.OsConstants
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format
import io.github.plpp555.spectracker.util.readFileOrNull

object MemoryCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val am = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val meminfo = readMemInfo()

        group("RAM") {
            row("合計") { Format.bytes(info.totalMem) }
            row("使用可能") { Format.bytes(info.availMem) }
            row("使用中") {
                val used = info.totalMem - info.availMem
                "${Format.bytes(used)}（${Format.percent(used * 100.0 / info.totalMem)}）"
            }
            optionalRow("未使用 (MemFree)") { meminfo["MemFree"]?.let { Format.bytes(it) } }
            optionalRow("バッファ") { meminfo["Buffers"]?.let { Format.bytes(it) } }
            optionalRow("キャッシュ") { meminfo["Cached"]?.let { Format.bytes(it) } }
            row("低メモリ状態") { Format.yesNo(info.lowMemory) }
            row("低メモリのしきい値") { Format.bytes(info.threshold) }
        }
        val swapTotal = meminfo["SwapTotal"]
        if (swapTotal != null && swapTotal > 0) {
            group("スワップ / zRAM") {
                row("合計") { Format.bytes(swapTotal) }
                optionalRow("空き") { meminfo["SwapFree"]?.let { Format.bytes(it) } }
                optionalRow("使用中") { meminfo["SwapFree"]?.let { Format.bytes(swapTotal - it) } }
            }
        }
        group("アプリのメモリ制限") {
            row("ヒープ上限（通常）") { "${am.memoryClass} MB" }
            row("ヒープ上限（largeHeap）") { "${am.largeMemoryClass} MB" }
            row("このアプリの最大ヒープ") { Format.bytes(Runtime.getRuntime().maxMemory()) }
            row("ローメモリ端末") { Format.yesNo(am.isLowRamDevice) }
            row("ページサイズ") { "${Os.sysconf(OsConstants._SC_PAGESIZE) / 1024} KB" }
        }
    }

    fun totalText(context: Context): String? {
        val am = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return Format.bytes(info.totalMem)
    }

    /** /proc/meminfo を「項目名 → バイト数」にして返す。 */
    private fun readMemInfo(): Map<String, Long> {
        val text = readFileOrNull("/proc/meminfo") ?: return emptyMap()
        return buildMap {
            for (line in text.lineSequence()) {
                val colon = line.indexOf(':')
                if (colon < 0) continue
                val kb = line.substring(colon + 1).trim().removeSuffix("kB").trim().toLongOrNull() ?: continue
                put(line.substring(0, colon).trim(), kb * 1024)
            }
        }
    }
}
