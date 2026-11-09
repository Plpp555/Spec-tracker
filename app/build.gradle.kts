import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// アプリ情報
// ---------------------------------------------------------------------------
val appVersionName = "V1" // 試作版 V1
val appVersionCode = 1

base {
    // 出力ファイル名: Spec-tracker-V1-debug.apk など
    archivesName.set("Spec-tracker-$appVersionName")
}

// ---------------------------------------------------------------------------
// 署名鍵の読み込み（debug と release は別の鍵。どちらも Git には含めない）
//
//   1. 環境変数   SPEC_TRACKER_<DEBUG|RELEASE>_{STORE_FILE,STORE_PASSWORD,KEY_ALIAS,KEY_PASSWORD}
//   2. ファイル   signing/<debug|release>.properties
//
// 詳しくは docs/SIGNING.md を参照してください。
// ---------------------------------------------------------------------------
class SigningKey(
    val storeFile: File,
    val storeType: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun loadSigningKey(kind: String): SigningKey? {
    val signingDir = rootProject.file("signing")
    val props = Properties()
    val propsFile = File(signingDir, "$kind.properties")
    if (propsFile.isFile) propsFile.inputStream().use { props.load(it) }

    val envPrefix = "SPEC_TRACKER_${kind.uppercase()}_"
    fun value(propKey: String, envKey: String): String? =
        System.getenv(envPrefix + envKey)?.takeIf { it.isNotBlank() }
            ?: props.getProperty(propKey)?.takeIf { it.isNotBlank() }

    val storePath = value("storeFile", "STORE_FILE") ?: return null
    val storeFile = File(storePath).let { if (it.isAbsolute) it else File(signingDir, storePath) }
    if (!storeFile.isFile) {
        throw GradleException("$kind 用の鍵ファイルが見つかりません: $storeFile")
    }
    val storePassword = value("storePassword", "STORE_PASSWORD")
        ?: throw GradleException("$kind 用の storePassword が未設定です")
    val keyAlias = value("keyAlias", "KEY_ALIAS")
        ?: throw GradleException("$kind 用の keyAlias が未設定です")
    return SigningKey(
        storeFile = storeFile,
        storeType = value("storeType", "STORE_TYPE") ?: "PKCS12",
        storePassword = storePassword,
        keyAlias = keyAlias,
        keyPassword = value("keyPassword", "KEY_PASSWORD") ?: storePassword,
    )
}

val debugKey = loadSigningKey("debug")
val releaseKey = loadSigningKey("release")

android {
    namespace = "io.github.plpp555.spectracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.plpp555.spectracker"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // debug: 非公開の debug 鍵で署名する。鍵が無い場合のみ、Gradle 標準の
        // ローカル debug 鍵（各開発者の PC で自動生成されるもの）にフォールバックする。
        if (debugKey != null) {
            getByName("debug") {
                storeFile = debugKey.storeFile
                storeType = debugKey.storeType
                storePassword = debugKey.storePassword
                keyAlias = debugKey.keyAlias
                keyPassword = debugKey.keyPassword
            }
        } else {
            logger.warn(
                "WARNING: signing/debug.properties が見つかりません。Gradle 標準のローカル debug 鍵で署名します。" +
                    "（この APK は配布用 debug 鍵のビルドとは更新互換性がありません）"
            )
        }
        // release: debug とは完全に別の鍵。鍵が無い場合は署名なしの APK になる
        // （debug 鍵で署名されることは決してない）。
        if (releaseKey != null) {
            create("release") {
                storeFile = releaseKey.storeFile
                storeType = releaseKey.storeType
                storePassword = releaseKey.storePassword
                keyAlias = releaseKey.keyAlias
                keyPassword = releaseKey.keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // release と別鍵のため、同じ端末に共存できるよう別のアプリ ID にする
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
