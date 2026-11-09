package io.github.plpp555.spectracker.collector

import android.content.Context
import android.os.Build
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.UNAVAILABLE
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.CpuNames
import io.github.plpp555.spectracker.util.Format
import io.github.plpp555.spectracker.util.SystemProps
import io.github.plpp555.spectracker.util.readFileOrNull
import io.github.plpp555.spectracker.util.readLongOrNull

object CpuCollector {
    private const val CPU_DIR = "/sys/devices/system/cpu"

    private class Core(
        val index: Int,
        val online: Boolean,
        val name: String?,
        val currentKHz: Long?,
        val maxKHz: Long?,
        val governor: String?,
    )

    private class CpuInfo(val perCore: Map<Int, Map<String, String>>, val global: Map<String, String>)

    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val cpuInfo = parseCpuInfo()
        val cores = readCores(cpuInfo)

        group("プロセッサ") {
            optionalRow("SoC メーカー") {
                if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER.takeUnless { it == Build.UNKNOWN } else SystemProps["ro.soc.manufacturer"]
            }
            optionalRow("SoC モデル") {
                if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL.takeUnless { it == Build.UNKNOWN } else SystemProps["ro.soc.model"]
            }
            optionalRow("プラットフォーム名") { SystemProps["ro.board.platform"] }
            optionalRow("ハードウェア名 (cpuinfo)") { cpuInfo.global["Hardware"] }
            row("アーキテクチャ") { System.getProperty("os.arch") }
            row("対応 ABI") { Build.SUPPORTED_ABIS.joinToString(", ") }
            optionalRow("64 ビット ABI") { Build.SUPPORTED_64_BIT_ABIS.joinToString(", ").ifEmpty { "非対応" } }
            row("コア数") {
                val total = cores.size
                val available = Runtime.getRuntime().availableProcessors()
                if (total > 0 && total != available) "$total（現在利用可能: $available）" else "$available"
            }
            optionalRow("クラスタ構成") { clusterSummary(cores) }
            optionalRow("CPU 機能 (Features)") { cpuInfo.perCore.values.firstOrNull()?.get("Features") }
        }
        group("コア別（現在の周波数 / 最大周波数 / ガバナー）") {
            for (core in cores) {
                row("CPU ${core.index}") { describeCore(core) }
            }
        }
    }

    /** 概要タブ用: 「8 コア（最大 3.30 GHz）」のような短い要約。 */
    fun summary(context: Context): String? {
        val cores = readCores(parseCpuInfo())
        val count = cores.size.takeIf { it > 0 } ?: Runtime.getRuntime().availableProcessors()
        val maxKHz = cores.mapNotNull { it.maxKHz }.maxOrNull()
        return if (maxKHz != null) "$count コア（最大 ${Format.frequencyKHz(maxKHz)}）" else "$count コア"
    }

    private fun describeCore(core: Core): String? {
        if (!core.online) return "オフライン（省電力のため停止中）"
        val parts = mutableListOf<String>()
        core.name?.let { parts += it }
        val cur = core.currentKHz?.let { Format.frequencyKHz(it) } ?: UNAVAILABLE
        val max = core.maxKHz?.let { Format.frequencyKHz(it) } ?: UNAVAILABLE
        parts += "$cur / $max"
        core.governor?.let { parts += it }
        return parts.joinToString("｜")
    }

    private fun clusterSummary(cores: List<Core>): String? {
        val withFreq = cores.filter { it.maxKHz != null }
        if (withFreq.isEmpty()) return null
        return withFreq
            .groupBy { it.maxKHz to it.name }
            .entries
            .sortedBy { it.key.first }
            .joinToString(" + ") { (key, group) ->
                val label = key.second?.let { " $it" }.orEmpty()
                "${group.size}× ${Format.frequencyKHz(key.first!!)}$label"
            }
    }

    private fun readCores(cpuInfo: CpuInfo): List<Core> {
        val count = possibleCpuCount() ?: Runtime.getRuntime().availableProcessors()
        return (0 until count).map { index ->
            val dir = "$CPU_DIR/cpu$index"
            val online = readFileOrNull("$dir/online")?.let { it == "1" } ?: true
            val info = cpuInfo.perCore[index]
            val implementer = info?.get("CPU implementer")?.parseHex()
            val part = info?.get("CPU part")?.parseHex()
            Core(
                index = index,
                online = online,
                name = if (implementer != null && part != null) CpuNames.describe(implementer, part) else null,
                currentKHz = readLongOrNull("$dir/cpufreq/scaling_cur_freq"),
                maxKHz = readLongOrNull("$dir/cpufreq/cpuinfo_max_freq"),
                governor = readFileOrNull("$dir/cpufreq/scaling_governor"),
            )
        }
    }

    /** "0-7" や "0-3,5" のような表記から CPU の総数を求める。 */
    private fun possibleCpuCount(): Int? {
        val text = readFileOrNull("$CPU_DIR/possible") ?: return null
        var total = 0
        for (part in text.split(',')) {
            val range = part.trim().split('-')
            val start = range[0].toIntOrNull() ?: return null
            val end = range.getOrNull(1)?.toIntOrNull() ?: start
            total += end - start + 1
        }
        return total.takeIf { it > 0 }
    }

    private fun parseCpuInfo(): CpuInfo {
        val text = readFileOrNull("/proc/cpuinfo") ?: return CpuInfo(emptyMap(), emptyMap())
        val perCore = mutableMapOf<Int, MutableMap<String, String>>()
        val global = mutableMapOf<String, String>()
        var current: MutableMap<String, String>? = null
        for (line in text.lineSequence()) {
            val colon = line.indexOf(':')
            if (colon < 0) {
                current = null
                continue
            }
            val key = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            if (key == "processor") {
                val index = value.toIntOrNull() ?: continue
                current = perCore.getOrPut(index) { mutableMapOf() }
            } else if (current != null) {
                current[key] = value
            } else {
                global[key] = value
            }
        }
        return CpuInfo(perCore, global)
    }

    private fun String.parseHex(): Int? = removePrefix("0x").removePrefix("0X").toIntOrNull(16)
}
