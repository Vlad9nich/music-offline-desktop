package com.yaneodex.desktop.integration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaFxPlaybackBackendContractTest {
    @Test
    fun `launch preflight passes when runtime classes are present`() {
        val result = JavaFxRuntime.verifyLaunchEnvironment()

        assertTrue(result.ok)
    }

    @Test
    fun `launch preflight reports missing runtime modules clearly`() {
        val classLoader = object : ClassLoader(JavaFxRuntime::class.java.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == "java.net.http.HttpClient" || name == "javafx.application.Platform" || name == "javafx.scene.media.MediaPlayer") {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }

        val result = JavaFxRuntime.verifyLaunchEnvironment(classLoader)

        assertFalse(result.ok)
        assertTrue(result.message!!.contains("MSI") || result.message.contains("EXE"))
        assertTrue(result.details!!.contains("java.net.http"))
        assertTrue(result.details.contains("javafx.application.Platform"))
        assertTrue(result.details.contains("javafx.scene.media.MediaPlayer"))
    }
}
