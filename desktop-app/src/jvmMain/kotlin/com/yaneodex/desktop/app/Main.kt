package com.yaneodex.desktop.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yaneodex.desktop.integration.JavaFxRuntime
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
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
        val appIcon = remember { buildAppIcon().toComposeImageBitmap() }
        Window(
            onCloseRequest = ::exitApplication,
            title = "YaNeoDex Desktop",
            icon = BitmapPainter(appIcon),
            // Without this the window opened at Compose's 800x600 default, which squeezed
            // the sidebar, the main column and the queue rail into each other.
            state = rememberWindowState(
                width = 1360.dp,
                height = 860.dp,
                position = WindowPosition(Alignment.Center),
            ),
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(1100, 720)
            }
            DesktopApp()
        }
    }
}

/**
 * The window/taskbar icon, drawn to match the in-app [com.yaneodex.desktop.ui.theme.YaNeoDexMark]:
 * a dark rounded plate with three level bars, the tallest in signal red.
 *
 * Previously the app shipped Compose Desktop's default placeholder icon, which is the first
 * thing you see in the taskbar and was the crudest-looking part of the whole product.
 */
private fun buildAppIcon(px: Int = 256): BufferedImage {
    val image = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

    val scale = px / 256f
    val plateInset = 6f * scale
    val plate = RoundRectangle2D.Float(
        plateInset,
        plateInset,
        px - plateInset * 2f,
        px - plateInset * 2f,
        58f * scale,
        58f * scale,
    )
    g.color = java.awt.Color(0x0A, 0x0A, 0x0A)
    g.fill(plate)
    g.color = java.awt.Color(0xFF, 0xFF, 0xFF, 0x1F)
    g.stroke = BasicStroke(2f * scale)
    g.draw(plate)

    val barW = 38f * scale
    val gap = 26f * scale
    val total = barW * 3 + gap * 2
    val left = (px - total) / 2f
    val full = 132f * scale
    val heights = floatArrayOf(0.48f, 1f, 0.7f)
    val colors = arrayOf(
        java.awt.Color(0xFF, 0xFF, 0xFF),
        java.awt.Color(0xFF, 0x1A, 0x1A),
        java.awt.Color(0xFF, 0xFF, 0xFF),
    )
    for (i in 0..2) {
        val barH = full * heights[i]
        val x = left + i * (barW + gap)
        val y = (px - barH) / 2f
        g.color = colors[i]
        g.fill(
            RoundRectangle2D.Float(x, y, barW, barH, barW, barW),
        )
    }

    g.dispose()
    return image
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
