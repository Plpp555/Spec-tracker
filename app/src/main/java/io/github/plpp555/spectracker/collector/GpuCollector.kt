package io.github.plpp555.spectracker.collector

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.opengl.GLES20
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.buildGroups
import io.github.plpp555.spectracker.util.Format

object GpuCollector {
    class GlInfo(
        val renderer: String?,
        val vendor: String?,
        val version: String?,
        val shadingLanguage: String?,
        val extensions: List<String>,
        val maxTextureSize: Int?,
        val eglVendor: String?,
        val eglVersion: String?,
        val eglExtensions: List<String>,
    )

    /** GPU の情報は変化しないため、プロセス内で 1 回だけ問い合わせる。 */
    private val glInfo: GlInfo? by lazy { queryGl() }

    fun collect(context: Context): List<SpecGroup> = buildGroups {
        val gl = glInfo
        group("GPU（OpenGL ES）") {
            row("GPU（レンダラー）") { gl?.renderer }
            row("ベンダー") { gl?.vendor }
            row("OpenGL ES バージョン") { gl?.version }
            optionalRow("GLSL バージョン") { gl?.shadingLanguage }
            optionalRow("最大テクスチャサイズ") { gl?.maxTextureSize?.let { "$it × $it px" } }
            row("端末が対応する OpenGL ES") {
                context.getSystemService(ActivityManager::class.java).deviceConfigurationInfo.glEsVersion
            }
            row("OpenGL ES 3.1 拡張パック (AEP)") { Format.supported(context.hasFeature("android.hardware.opengles.aep")) }
        }
        group("Vulkan") {
            val pm = context.packageManager
            row("Vulkan") {
                val version = pm.featureVersion("android.hardware.vulkan.version")
                if (version != null) "対応（API ${decodeVulkanVersion(version)}）" else Format.supported(false)
            }
            row("Vulkan ハードウェアレベル") {
                pm.featureVersion("android.hardware.vulkan.level")?.let { "レベル $it" } ?: Format.supported(false)
            }
            row("Vulkan コンピュート") { Format.supported(context.hasFeature("android.hardware.vulkan.compute")) }
        }
        group("EGL") {
            optionalRow("EGL ベンダー") { gl?.eglVendor }
            optionalRow("EGL バージョン") { gl?.eglVersion }
            optionalRow("EGL 拡張機能数") { gl?.eglExtensions?.size?.takeIf { it > 0 }?.toString() }
        }
        group("拡張機能") {
            optionalRow("OpenGL ES 拡張機能数") { gl?.extensions?.size?.takeIf { it > 0 }?.toString() }
            optionalRow("OpenGL ES 拡張機能一覧") { gl?.extensions?.sorted()?.joinToString("\n") }
        }
    }

    fun rendererOrNull(): String? = glInfo?.renderer

    /** Vulkan の API バージョン（VK_MAKE_VERSION 形式）を "1.3.0" のような文字列にする。 */
    internal fun decodeVulkanVersion(encoded: Int): String =
        "${(encoded shr 22) and 0x7F}.${(encoded shr 12) and 0x3FF}.${encoded and 0xFFF}"

    /**
     * 1x1 の pbuffer を持つ一時的な OpenGL ES コンテキストを作り、GPU の情報を読み取る。
     * 画面描画（HWUI）と同じ EGLDisplay を共有しているため、eglTerminate は呼ばない
     * （呼ぶと描画側の EGL を巻き込む恐れがある）。作ったコンテキストとサーフェスだけ破棄する。
     */
    private fun queryGl(): GlInfo? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return null
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null

        var context: EGLContext = EGL14.EGL_NO_CONTEXT
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
        try {
            val configAttributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val configCount = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, configCount, 0) || configCount[0] == 0) {
                return null
            }
            val config = configs[0] ?: return null

            context = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
            )
            surface = EGL14.eglCreatePbufferSurface(
                display, config,
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
            )
            if (context == EGL14.EGL_NO_CONTEXT || surface == EGL14.EGL_NO_SURFACE) return null
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return null

            val textureSize = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, textureSize, 0)
            return GlInfo(
                renderer = GLES20.glGetString(GLES20.GL_RENDERER),
                vendor = GLES20.glGetString(GLES20.GL_VENDOR),
                version = GLES20.glGetString(GLES20.GL_VERSION),
                shadingLanguage = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION),
                extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS).splitWords(),
                maxTextureSize = textureSize[0].takeIf { it > 0 },
                eglVendor = EGL14.eglQueryString(display, EGL14.EGL_VENDOR),
                eglVersion = EGL14.eglQueryString(display, EGL14.EGL_VERSION),
                eglExtensions = EGL14.eglQueryString(display, EGL14.EGL_EXTENSIONS).splitWords(),
            )
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
        }
    }

    private fun String?.splitWords(): List<String> =
        this?.split(' ')?.filter { it.isNotBlank() }.orEmpty()
}
