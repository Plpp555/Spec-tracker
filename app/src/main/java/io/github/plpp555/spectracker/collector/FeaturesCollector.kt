package io.github.plpp555.spectracker.collector

import android.content.Context
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups

object FeaturesCollector {
    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val pm = context.packageManager
        val features = pm.systemAvailableFeatures.filter { it.name != null }.sortedBy { it.name }
        group("システム機能（${features.size} 件）") {
            for (feature in features) {
                row(feature.name!!.removePrefix("android."), if (feature.version != 0) "v${feature.version}" else "対応")
            }
        }
        val libraries = pm.systemSharedLibraryNames?.sorted().orEmpty()
        if (libraries.isNotEmpty()) {
            group("システム共有ライブラリ（${libraries.size} 件）") {
                row("ライブラリ一覧", libraries.joinToString("\n"))
            }
        }
    }
}
