package com.yaneodex.desktop.integration

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

data class LaunchPreflightResult(
    val ok: Boolean,
    val title: String? = null,
    val message: String? = null,
    val details: String? = null,
)

object JavaFxRuntime {
    private val initialized = AtomicBoolean(false)

    fun verifyLaunchEnvironment(classLoader: ClassLoader = JavaFxRuntime::class.java.classLoader): LaunchPreflightResult {
        val missing = mutableListOf<String>()

        if (!isClassAvailable(classLoader, "java.net.http.HttpClient")) {
            missing += "java.net.http"
        }
        if (!isClassAvailable(classLoader, "javafx.application.Platform")) {
            missing += "javafx.application.Platform"
        }
        if (!isClassAvailable(classLoader, "javafx.scene.media.MediaPlayer")) {
            missing += "javafx.scene.media.MediaPlayer"
        }

        if (missing.isNotEmpty()) {
            return LaunchPreflightResult(
                ok = false,
                title = "Приложение установлено не полностью",
                message = "Переустанови YaNeoDex Desktop через свежий MSI или EXE.",
                details = "Не найдены runtime-модули: ${missing.joinToString()}",
            )
        }

        return LaunchPreflightResult(ok = true)
    }

    fun ensureInitialized() {
        if (!initialized.compareAndSet(false, true)) return

        val latch = CountDownLatch(1)
        var startupError: Throwable? = null
        try {
            Platform.startup {
                Platform.setImplicitExit(false)
                latch.countDown()
            }
        } catch (_: IllegalStateException) {
            Platform.setImplicitExit(false)
            latch.countDown()
        } catch (error: Throwable) {
            startupError = error
            latch.countDown()
        }
        latch.await()
        startupError?.let { throw it }
    }

    private fun isClassAvailable(classLoader: ClassLoader, name: String): Boolean =
        runCatching { Class.forName(name, false, classLoader) }.isSuccess
}
