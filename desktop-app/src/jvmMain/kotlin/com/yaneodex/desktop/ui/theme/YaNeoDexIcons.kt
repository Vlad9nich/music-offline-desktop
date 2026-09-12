package com.yaneodex.desktop.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's own icon set.
 *
 * Everything here is drawn on a 24x24 grid with one stroke weight, round caps and round
 * joins. That single decision is what fixes the "axe-cut" feel: the previous set mixed
 * Material's filled and outlined glyphs at 16-19dp, so a solid "library" block sat next to
 * a hairline "home", and detail-heavy shapes (playlist-with-arrow, sparkles for "import")
 * turned to mush at that size.
 *
 * Drawing them ourselves also means the set is deliberately small and geometric — nothing
 * has more than a handful of strokes, so it still reads at 18dp.
 */
enum class YdxGlyph {
    Home,
    Search,
    Playlist,
    Queue,
    Library,
    Settings,
    Panel,
    Play,
    Pause,
    Prev,
    Next,
    Shuffle,
    Volume,
    VolumeOff,
    Back,
    Forward,
    ChevronDown,
    ChevronUp,
    Plus,
    Close,
    Edit,
    Delete,
    Folder,
    Refresh,
    Globe,
    Check,
    CheckCircle,
    Circle,
}

/**
 * Renders a [YdxGlyph].
 *
 * [boxSize] is the square the glyph is drawn into; [stroke] defaults to roughly 1.5dp at a
 * 20dp box, which keeps the optical weight constant across sizes.
 */
@Composable
fun YdxIcon(
    glyph: YdxGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 20.dp,
    stroke: Dp = boxSize * 0.075f,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = modifier.then(semanticsModifier).size(boxSize)) {
        val unit = minOf(size.width, size.height) / 24f
        drawGlyph(glyph, tint, unit, stroke.toPx())
    }
}

private fun DrawScope.drawGlyph(glyph: YdxGlyph, color: Color, u: Float, sw: Float) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
        color = color,
        start = p(x1, y1),
        end = p(x2, y2),
        strokeWidth = sw,
        cap = StrokeCap.Round,
    )

    fun poly(vararg points: Float) {
        val path = Path()
        path.moveTo(points[0] * u, points[1] * u)
        var i = 2
        while (i < points.size) {
            path.lineTo(points[i] * u, points[i + 1] * u)
            i += 2
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    fun outline(vararg points: Float) {
        val path = Path()
        path.moveTo(points[0] * u, points[1] * u)
        var i = 2
        while (i < points.size) {
            path.lineTo(points[i] * u, points[i + 1] * u)
            i += 2
        }
        path.close()
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    fun solid(vararg points: Float) {
        val path = Path()
        path.moveTo(points[0] * u, points[1] * u)
        var i = 2
        while (i < points.size) {
            path.lineTo(points[i] * u, points[i + 1] * u)
            i += 2
        }
        path.close()
        drawPath(path, color)
    }

    fun ring(cx: Float, cy: Float, r: Float) =
        drawCircle(color, radius = r * u, center = p(cx, cy), style = Stroke(width = sw))

    fun disc(cx: Float, cy: Float, r: Float) = drawCircle(color, radius = r * u, center = p(cx, cy))

    fun bar(x: Float, y: Float, barW: Float, barH: Float, radius: Float) = drawRoundRect(
        color = color,
        topLeft = p(x, y),
        size = Size(barW * u, barH * u),
        cornerRadius = CornerRadius(radius * u),
    )

    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) = drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = p(cx - r, cy - r),
        size = Size(2 * r * u, 2 * r * u),
        style = Stroke(width = sw, cap = StrokeCap.Round),
    )

    when (glyph) {
        // --- navigation -------------------------------------------------------------

        // Roof plus an open body: two strokes, no door, no windows.
        YdxGlyph.Home -> {
            poly(3.5f, 11.2f, 12f, 4.2f, 20.5f, 11.2f)
            poly(6.3f, 10.2f, 6.3f, 19.5f, 17.7f, 19.5f, 17.7f, 10.2f)
        }

        YdxGlyph.Search -> {
            ring(10.4f, 10.4f, 6.2f)
            line(15f, 15f, 20f, 20f)
        }

        // Three rows plus a play head.
        YdxGlyph.Playlist -> {
            line(3.5f, 6.5f, 13.5f, 6.5f)
            line(3.5f, 12f, 13.5f, 12f)
            line(3.5f, 17.5f, 13.5f, 17.5f)
            solid(16.5f, 8.6f, 21f, 12f, 16.5f, 15.4f)
        }

        // Three plain rows — the queue rail.
        YdxGlyph.Queue -> {
            line(3.5f, 6.5f, 20.5f, 6.5f)
            line(3.5f, 12f, 20.5f, 12f)
            line(3.5f, 17.5f, 20.5f, 17.5f)
        }

        // A single quaver: one head, one stem, one flag.
        YdxGlyph.Library -> {
            ring(8.7f, 16.3f, 3f)
            line(11.7f, 16.3f, 11.7f, 5f)
            poly(11.7f, 5f, 18.5f, 7.4f)
        }

        // Arrow dropping into a tray — "import", not sparkles.
        // Two sliders: a rail broken by a knob.
        YdxGlyph.Settings -> {
            line(3.5f, 8.5f, 6.6f, 8.5f)
            ring(9.6f, 8.5f, 2.7f)
            line(12.6f, 8.5f, 20.5f, 8.5f)
            line(3.5f, 15.5f, 12.4f, 15.5f)
            ring(15.4f, 15.5f, 2.7f)
            line(18.4f, 15.5f, 20.5f, 15.5f)
        }

        // Sidebar toggle: a panel with its divider near the left edge.
        YdxGlyph.Panel -> {
            drawRoundRect(
                color = color,
                topLeft = p(3.2f, 4.2f),
                size = Size(17.6f * u, 15.6f * u),
                cornerRadius = CornerRadius(3f * u),
                style = Stroke(width = sw),
            )
            line(9.4f, 4.2f, 9.4f, 19.8f)
        }

        // --- transport --------------------------------------------------------------

        YdxGlyph.Play -> solid(7.4f, 4f, 20.2f, 12f, 7.4f, 20f)
        YdxGlyph.Pause -> {
            bar(8f, 4.8f, 3.2f, 14.4f, 1.6f)
            bar(12.8f, 4.8f, 3.2f, 14.4f, 1.6f)
        }
        YdxGlyph.Prev -> {
            bar(6f, 5f, 2.9f, 14f, 1.45f)
            solid(19f, 5f, 9.6f, 12f, 19f, 19f)
        }
        YdxGlyph.Next -> {
            bar(15.1f, 5f, 2.9f, 14f, 1.45f)
            solid(5f, 5f, 14.4f, 12f, 5f, 19f)
        }

        // Two lanes crossing, one arrowhead per lane. Deliberately airy: this glyph is the
        // busiest in the set and at 20dp it turns into a scribble if the lanes run tight.
        YdxGlyph.Shuffle -> {
            poly(3.2f, 7f, 8.4f, 7f, 15.6f, 17f, 20.2f, 17f)
            poly(17.6f, 14.6f, 20.2f, 17f, 17.6f, 19.4f)
            poly(3.2f, 17f, 8.4f, 17f, 15.6f, 7f, 20.2f, 7f)
            poly(17.6f, 4.6f, 20.2f, 7f, 17.6f, 9.4f)
        }

        YdxGlyph.Volume -> {
            outline(3.2f, 9.6f, 6.8f, 9.6f, 10.6f, 5.2f, 10.6f, 18.8f, 6.8f, 14.4f, 3.2f, 14.4f)
            arc(11.6f, 12f, 3.6f, -55f, 110f)
            arc(11.6f, 12f, 6.4f, -55f, 110f)
        }

        YdxGlyph.VolumeOff -> {
            outline(3.2f, 9.6f, 6.8f, 9.6f, 10.6f, 5.2f, 10.6f, 18.8f, 6.8f, 14.4f, 3.2f, 14.4f)
            line(14.6f, 9.4f, 20.2f, 14.8f)
            line(20.2f, 9.4f, 14.6f, 14.8f)
        }

        // --- chrome -----------------------------------------------------------------

        YdxGlyph.Back -> poly(14.4f, 4.6f, 7f, 12f, 14.4f, 19.4f)
        YdxGlyph.Forward -> poly(9.6f, 4.6f, 17f, 12f, 9.6f, 19.4f)
        YdxGlyph.ChevronDown -> poly(5.6f, 9.4f, 12f, 15.8f, 18.4f, 9.4f)
        YdxGlyph.ChevronUp -> poly(5.6f, 14.6f, 12f, 8.2f, 18.4f, 14.6f)
        YdxGlyph.Plus -> {
            line(12f, 4.6f, 12f, 19.4f)
            line(4.6f, 12f, 19.4f, 12f)
        }
        YdxGlyph.Close -> {
            line(5.6f, 5.6f, 18.4f, 18.4f)
            line(18.4f, 5.6f, 5.6f, 18.4f)
        }

        // Pencil body plus the ferrule seam.
        YdxGlyph.Edit -> {
            outline(4.4f, 19.6f, 5.6f, 15.2f, 15.9f, 4.9f, 19.1f, 8.1f, 8.8f, 18.4f)
            line(14.4f, 6.4f, 17.6f, 9.6f)
        }

        YdxGlyph.Delete -> {
            line(3.8f, 6.8f, 20.2f, 6.8f)
            poly(9f, 6.8f, 9f, 4.2f, 15f, 4.2f, 15f, 6.8f)
            poly(6.2f, 6.8f, 7.4f, 20f, 16.6f, 20f, 17.8f, 6.8f)
            line(10.2f, 10.2f, 10.2f, 16.8f)
            line(13.8f, 10.2f, 13.8f, 16.8f)
        }

        YdxGlyph.Folder -> {
            outline(3.5f, 18.8f, 3.5f, 5.8f, 9.6f, 5.8f, 11.6f, 8.9f, 20.5f, 8.9f, 20.5f, 18.8f)
        }

        // Open arc with a tangent head — reads as "reload" without a gear.
        YdxGlyph.Refresh -> {
            arc(12f, 12f, 7f, -52f, 288f)
            solid(17.9f, 7.2f, 16.7f, 3.9f, 14.3f, 7.9f)
        }

        YdxGlyph.Globe -> {
            ring(12f, 12f, 7.6f)
            drawOval(
                color = color,
                topLeft = p(8.4f, 4.4f),
                size = Size(7.2f * u, 15.2f * u),
                style = Stroke(width = sw),
            )
            line(4.4f, 12f, 19.6f, 12f)
        }

        YdxGlyph.Check -> poly(4.8f, 12.4f, 9.8f, 17.4f, 19.2f, 6.6f)

        YdxGlyph.CheckCircle -> {
            ring(12f, 12f, 8f)
            poly(7.7f, 12.2f, 10.7f, 15.2f, 16.3f, 9.2f)
        }

        YdxGlyph.Circle -> ring(12f, 12f, 8f)
    }
}

/**
 * The YaNeoDex mark: three level bars, the tallest in signal red.
 *
 * Replaces the bare red dot that used to sit before the wordmark. Bars are drawn as
 * rounded rectangles so the mark survives being rendered at 16dp, and the whole thing is
 * geometry only — no text, no outline box, nothing to look crude at small sizes.
 */
@Composable
fun YaNeoDexMark(
    modifier: Modifier = Modifier,
    boxSize: Dp = 22.dp,
    color: Color = Wd2.Text,
    accent: Color = Wd2.Accent,
) {
    Canvas(modifier = modifier.size(boxSize)) {
        val w = size.width
        val h = size.height
        val barW = w * 0.2f
        val gap = w * 0.12f
        val total = barW * 3 + gap * 2
        val left = (w - total) / 2f
        val heights = floatArrayOf(0.48f, 1f, 0.7f)
        val colors = arrayOf(color, accent, color)
        for (i in 0..2) {
            val barH = h * heights[i]
            drawRoundRect(
                color = colors[i],
                topLeft = Offset(left + i * (barW + gap), (h - barH) / 2f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}
