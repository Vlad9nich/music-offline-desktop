package com.yaneodex.desktop.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yaneodex.desktop.integration.JavaFxRuntime

fun main() = application {
    JavaFxRuntime.ensureInitialized()
    Window(
        onCloseRequest = ::exitApplication,
        title = "YaNeoDex Desktop",
    ) {
        DesktopApp()
    }
}
