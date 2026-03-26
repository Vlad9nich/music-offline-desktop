package com.yaneodex.desktop.integration

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

object JavaFxRuntime {
    private val initialized = AtomicBoolean(false)

    fun ensureInitialized() {
        if (!initialized.compareAndSet(false, true)) return

        val latch = CountDownLatch(1)
        try {
            SwingUtilities.invokeAndWait {
                JFXPanel()
                Platform.setImplicitExit(false)
                latch.countDown()
            }
        } catch (_: Throwable) {
            try {
                Platform.startup {
                    Platform.setImplicitExit(false)
                    latch.countDown()
                }
            } catch (_: IllegalStateException) {
                latch.countDown()
            }
        }
        latch.await()
    }
}
