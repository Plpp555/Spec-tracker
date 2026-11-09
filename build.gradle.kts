// ルートプロジェクト。プラグインのバージョンは gradle/libs.versions.toml で一元管理します。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
