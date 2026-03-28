package com.yaneodex.desktop.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yaneodex.desktop.integration.JavaFxRuntime
import javax.swing.JOptionPane

fun main() {
    val preflight = JavaFxRuntime.verifyLaunchEnvironment()
    if (!preflight.ok) {
        showLaunchError(preflight.title ?: "Ошибка запуска", preflight.message ?: "Не удалось запустить приложение.", preflight.details)
        return
    }

    runCatching { JavaFxRuntime.ensureInitialized() }
        .onFailure { error ->
            showLaunchError(
                title = "Не удалось запустить аудио-модуль",
                message = "Переустанови приложение через свежий MSI или EXE.",
                details = error.message ?: error::class.simpleName,
            )
            return
        }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "YaNeoDex Desktop",
        ) {
            DesktopApp()
        }
    }
}

private fun showLaunchError(title: String, message: String, details: String?) {
    val body = buildString {
        appendLine(message)
        details?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            append(it)
        }
    }
    JOptionPane.showMessageDialog(null, body, title, JOptionPane.ERROR_MESSAGE)
}
