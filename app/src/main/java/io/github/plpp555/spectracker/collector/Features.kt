package io.github.plpp555.spectracker.collector

import android.content.Context
import android.content.pm.PackageManager

/** PackageManager のハードウェア/ソフトウェア機能フラグの確認。 */
internal fun Context.hasFeature(name: String): Boolean = packageManager.hasSystemFeature(name)

internal fun PackageManager.featureVersion(name: String): Int? =
    systemAvailableFeatures.firstOrNull { it.name == name }?.version
