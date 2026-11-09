package io.github.plpp555.spectracker.collector

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format
import io.github.plpp555.spectracker.util.readFileOrNull

object StorageCollector {
    private class Capacity(val total: Long, val free: Long)

    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val internal = internalCapacity(context)
        group("内部ストレージ") {
            row("合計容量") { Format.bytes(internal!!.total) }
            row("空き容量") { Format.bytes(internal!!.free) }
            row("使用中") {
                val used = internal!!.total - internal.free
                "${Format.bytes(used)}（${Format.percent(used * 100.0 / internal.total)}）"
            }
            optionalRow("データ領域 (/data) のファイルシステム") { filesystemOf("/data") }
        }
        group("ストレージボリューム") {
            val manager = context.getSystemService(StorageManager::class.java)
            for (volume in manager.storageVolumes) {
                row(volume.getDescription(context)) { describeVolume(volume) }
            }
        }
    }

    fun summary(context: Context): String? {
        val capacity = internalCapacity(context) ?: return null
        return "${Format.bytes(capacity.total)}（空き ${Format.bytes(capacity.free)}）"
    }

    /** 端末の公称に近い合計容量を返す StorageStatsManager を優先し、だめなら StatFs で代替する。 */
    private fun internalCapacity(context: Context): Capacity? =
        try {
            val stats = context.getSystemService(StorageStatsManager::class.java)
            Capacity(
                total = stats.getTotalBytes(StorageManager.UUID_DEFAULT),
                free = stats.getFreeBytes(StorageManager.UUID_DEFAULT),
            )
        } catch (e: Exception) {
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                Capacity(total = stat.totalBytes, free = stat.availableBytes)
            } catch (e2: Exception) {
                null
            }
        }

    private fun describeVolume(volume: StorageVolume): String {
        val traits = buildList {
            if (volume.isPrimary) add("プライマリ")
            add(if (volume.isRemovable) "取り外し可能" else "内蔵")
            if (volume.isEmulated) add("エミュレート")
        }
        val state = when (volume.state) {
            Environment.MEDIA_MOUNTED -> "マウント済み"
            Environment.MEDIA_MOUNTED_READ_ONLY -> "読み取り専用"
            Environment.MEDIA_UNMOUNTED -> "未マウント"
            Environment.MEDIA_REMOVED -> "取り外し済み"
            Environment.MEDIA_EJECTING -> "取り出し中"
            else -> volume.state
        }
        val capacity = if (Build.VERSION.SDK_INT >= 30 && volume.state == Environment.MEDIA_MOUNTED) {
            volume.directory?.let {
                try {
                    val stat = StatFs(it.path)
                    "合計 ${Format.bytes(stat.totalBytes)} / 空き ${Format.bytes(stat.availableBytes)}"
                } catch (e: Exception) {
                    null
                }
            }
        } else {
            null
        }
        return listOfNotNull(traits.joinToString("・"), state, capacity).joinToString("｜")
    }

    /** /proc/mounts から、指定したマウントポイントのファイルシステム名（f2fs / ext4 など）を調べる。 */
    private fun filesystemOf(mountPoint: String): String? =
        readFileOrNull("/proc/mounts")
            ?.lineSequence()
            ?.map { it.split(' ') }
            ?.firstOrNull { it.size >= 3 && it[1] == mountPoint }
            ?.get(2)
}
