package com.midotheeng.fullpressure

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// ---------- palette ----------
val WaterBlue = Color(0xFF3FA9F5)
private val WaterDeep = Color(0xFF1668B8)
private val WaterLight = Color(0xFF8FDBFF)
private val TubeCore = Color(0xFF0B1522)
private val BrassDark = Color(0xFF6E5012)
private val BrassMid = Color(0xFFB38B2C)
private val BrassLight = Color(0xFFF0CF77)
private val ChromeDark = Color(0xFF6F7B88)
private val ChromeLight = Color(0xFFF4F7FA)
private val ChromeMid = Color(0xFFA4AFBA)
private val CeramicWhite = Color(0xFFFFFFFF)
private val CeramicShade = Color(0xFFC3CDD7)
private val SteelDark = Color(0xFF56667A)
private val SteelLight = Color(0xFFD3DCE5)
private val SteelMid = Color(0xFF8A9AAE)

private val waterAcross = listOf(WaterDeep, WaterBlue, WaterLight, WaterBlue, WaterDeep)
private val glassAcross = listOf(
    Color(0x70FFFFFF), Color(0x14FFFFFF), Color(0x00FFFFFF), Color(0x10FFFFFF), Color(0x55FFFFFF),
)
private val brassAcross = listOf(BrassDark, BrassLight, BrassMid, BrassDark)

const val PIPE_THICKNESS = 0.34f

private fun DrawScope.u(f: Float) = f * size.minDimension
private fun DrawScope.p(x: Float, y: Float) = Offset(u(x), u(y))
private fun DrawScope.sz(w: Float, h: Float) = Size(u(w), u(h))

/** Gradient running across an arm (perpendicular to its direction). */
private fun across(colors: List<Color>, horizontal: Boolean, a: Float, b: Float): Brush =
    if (horizontal) Brush.verticalGradient(colors, startY = a, endY = b)
    else Brush.horizontalGradient(colors, startX = a, endX = b)

// ---------- floor & walls ----------

fun DrawScope.drawFloor() {
    drawRect(Brush.linearGradient(
        listOf(Color(0xFF223650), Color(0xFF1A2B40)),
        start = Offset.Zero, end = Offset(size.width, size.height),
    ))
    // little glaze sheen in the corner of each tile
    drawLine(Color(0x22FFFFFF), p(0.12f, 0.1f), p(0.32f, 0.1f), strokeWidth = u(0.02f), cap = StrokeCap.Round)
}

fun DrawScope.drawBricks() {
    drawRect(Color(0xFF3A2620))
    val rows = 4
    val bh = size.height / rows
    val bw = size.width / 2f
    val gap = u(0.035f)
    for (r in 0 until rows) {
        val shift = if (r % 2 == 0) 0f else -bw / 2f
        var x = shift
        var k = 0
        while (x < size.width) {
            val shade = if ((r + k) % 3 == 0) Color(0xFF8A4A36) else if ((r + k) % 3 == 1) Color(0xFF7A3F2E) else Color(0xFF94523C)
            val top = r * bh + gap / 2
            drawRect(
                Brush.verticalGradient(listOf(shade.copy(red = shade.red * 1.15f), shade), startY = top, endY = top + bh - gap),
                topLeft = Offset(x + gap / 2, top),
                size = Size(bw - gap, bh - gap),
            )
            x += bw
            k++
        }
    }
}

// ---------- pipes ----------

/** One arm of glass pipe from [from] to [to] (0 = centre, 1 = cell edge). */
private fun DrawScope.tubeArm(d: Dir, from: Float, to: Float, wet: Boolean, flow: Float) {
    val half = size.minDimension / 2f
    val c = center
    val t = u(PIPE_THICKNESS)
    val a = Offset(c.x + d.dc * from * half, c.y + d.dr * from * half)
    val b = Offset(c.x + d.dc * to * half, c.y + d.dr * to * half)
    val horizontal = d.dr == 0
    val left = minOf(a.x, b.x)
    val right = maxOf(a.x, b.x)
    val top = minOf(a.y, b.y)
    val bottom = maxOf(a.y, b.y)
    val tl: Offset
    val s: Size
    val lo: Float
    val hi: Float
    if (horizontal) {
        tl = Offset(left, c.y - t / 2); s = Size(right - left, t); lo = c.y - t / 2; hi = c.y + t / 2
    } else {
        tl = Offset(c.x - t / 2, top); s = Size(t, bottom - top); lo = c.x - t / 2; hi = c.x + t / 2
    }
    drawRect(TubeCore, tl, s)
    if (wet) {
        val inset = t * 0.13f
        val wtl = if (horizontal) Offset(tl.x, tl.y + inset) else Offset(tl.x + inset, tl.y)
        val ws = if (horizontal) Size(s.width, s.height - 2 * inset) else Size(s.width - 2 * inset, s.height)
        drawRect(across(waterAcross, horizontal, lo + inset, hi - inset), wtl, ws)
        // moving streaks show the water flowing
        val on = u(0.05f)
        val off = u(0.09f)
        drawLine(
            Color(0x99FFFFFF), a, b, strokeWidth = t * 0.1f, cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off), -flow * (on + off)),
        )
    }
    drawRect(across(glassAcross, horizontal, lo, hi), tl, s)
    // glass edges
    val edge = Color(0x88FFFFFF)
    val w = u(0.018f)
    if (horizontal) {
        drawLine(edge, Offset(left, lo), Offset(right, lo), w)
        drawLine(Color(0x44FFFFFF), Offset(left, hi), Offset(right, hi), w)
    } else {
        drawLine(edge, Offset(lo, top), Offset(lo, bottom), w)
        drawLine(Color(0x44FFFFFF), Offset(hi, top), Offset(hi, bottom), w)
    }
}

/** Brass coupling where the pipe meets the cell edge. */
private fun DrawScope.coupling(d: Dir) {
    val half = size.minDimension / 2f
    val c = center
    val t = u(PIPE_THICKNESS) * 1.3f
    val len = u(0.085f)
    val horizontal = d.dr == 0
    val endDist = half
    val startDist = half - len
    val a = startDist
    val tl = if (horizontal) {
        Offset(if (d.dc > 0) c.x + a else c.x - endDist, c.y - t / 2)
    } else {
        Offset(c.x - t / 2, if (d.dr > 0) c.y + a else c.y - endDist)
    }
    val s = if (horizontal) Size(len, t) else Size(t, len)
    val lo = if (horizontal) c.y - t / 2 else c.x - t / 2
    drawRect(across(brassAcross, horizontal, lo, lo + t), tl, s)
    // nut lines
    val line = Color(0x55000000)
    if (horizontal) {
        val x = tl.x + len / 2
        drawLine(line, Offset(x, tl.y), Offset(x, tl.y + t), u(0.012f))
    } else {
        val y = tl.y + len / 2
        drawLine(line, Offset(tl.x, y), Offset(tl.x + t, y), u(0.012f))
    }
}

/** A pipe piece with openings [mask], drawn in its current orientation. */
fun DrawScope.drawPipe(mask: Int, wet: Boolean, flow: Float) {
    val straight = mask == (N or S) || mask == (E or W)
    for (d in DIRS) if (mask and d.bit != 0) tubeArm(d, 0f, 1f, wet, flow)
    if (!straight) {
        val r = u(PIPE_THICKNESS) * 0.64f
        drawCircle(
            Brush.radialGradient(listOf(BrassLight, BrassMid, BrassDark), center = Offset(center.x - r * 0.3f, center.y - r * 0.3f), radius = r * 1.4f),
            r, center,
        )
        drawCircle(Color(0x66000000), r * 0.62f, center, style = Stroke(u(0.015f)))
        if (wet) drawCircle(WaterBlue.copy(alpha = 0.85f), r * 0.42f, center)
        else drawCircle(TubeCore, r * 0.42f, center)
    }
    for (d in DIRS) if (mask and d.bit != 0) coupling(d)
}

/** Short pipe stubs from a tank/fixture body out to the sides that connect. */
fun DrawScope.drawStubs(sides: Int, wet: Boolean, flow: Float) {
    for (d in DIRS) if (sides and d.bit != 0) {
        tubeArm(d, 0.5f, 1f, wet, flow)
        coupling(d)
    }
}

/** Water spraying out of open pipe ends. */
fun DrawScope.drawLeaks(mask: Int, phase: Float) {
    val half = size.minDimension / 2f
    for (d in DIRS) {
        if (mask and d.bit == 0) continue
        val e = Offset(center.x + d.dc * half * 0.9f, center.y + d.dr * half * 0.9f)
        val perp = Offset(-d.dr.toFloat(), d.dc.toFloat())
        val back = Offset(-d.dc.toFloat(), -d.dr.toFloat())
        drawOval(
            WaterBlue.copy(alpha = 0.35f),
            topLeft = Offset(e.x - u(0.2f), e.y - u(0.12f)),
            size = sz(0.4f, 0.24f),
        )
        for (k in 0 until 10) {
            val q = (phase + k / 10f) % 1f
            val spread = ((k % 5) - 2f) / 2f
            val pos = e + back * (q * u(0.28f)) + perp * (spread * q * u(0.34f))
            drawCircle(WaterLight.copy(alpha = (1f - q) * 0.9f), u(0.035f) * (1f - q * 0.5f), pos)
        }
    }
}

// ---------- tank ----------

fun DrawScope.drawTank(wobble: Float) {
    val steel = Brush.horizontalGradient(
        listOf(SteelDark, SteelLight, SteelMid, SteelDark), startX = u(0.2f), endX = u(0.8f),
    )
    drawRoundRect(steel, p(0.2f, 0.12f), sz(0.6f, 0.76f), CornerRadius(u(0.14f)))
    // domed top
    drawOval(
        Brush.verticalGradient(listOf(Color(0xFFE8EEF4), SteelMid), startY = u(0.1f), endY = u(0.24f)),
        p(0.22f, 0.11f), sz(0.56f, 0.13f),
    )
    // bands
    for (y in listOf(0.34f, 0.72f)) {
        drawLine(Color(0x55000000), p(0.2f, y), p(0.8f, y), u(0.022f))
        drawLine(Color(0x33FFFFFF), p(0.2f, y + 0.018f), p(0.8f, y + 0.018f), u(0.01f))
    }
    // sight glass with the water level
    drawRoundRect(Color(0xFF0B1522), p(0.43f, 0.3f), sz(0.14f, 0.44f), CornerRadius(u(0.05f)))
    val level = 0.42f + wobble * 0.02f
    drawRoundRect(
        Brush.verticalGradient(listOf(WaterLight, WaterBlue, WaterDeep), startY = u(level), endY = u(0.73f)),
        p(0.445f, level), sz(0.11f, 0.73f - level), CornerRadius(u(0.04f)),
    )
    drawLine(Color(0x66FFFFFF), p(0.455f, 0.32f), p(0.455f, 0.72f), u(0.012f))
}

// ---------- fixtures ----------

private fun DrawScope.chrome(x0: Float, x1: Float) =
    Brush.horizontalGradient(listOf(ChromeDark, ChromeLight, ChromeMid), startX = u(x0), endX = u(x1))

private fun DrawScope.ceramic(y0: Float, y1: Float) =
    Brush.verticalGradient(listOf(CeramicWhite, CeramicShade), startY = u(y0), endY = u(y1))

private fun DrawScope.ripple(cx: Float, cy: Float, phase: Float, max: Float) {
    drawCircle(Color.White.copy(alpha = (1f - phase) * 0.7f), u(0.03f + phase * max), p(cx, cy), style = Stroke(u(0.012f)))
}

fun DrawScope.drawSink(active: Boolean, phase: Float) {
    drawOval(ceramic(0.3f, 0.8f), p(0.15f, 0.3f), sz(0.7f, 0.5f))
    drawOval(
        Brush.radialGradient(listOf(Color(0xFFEFF3F6), Color(0xFF9AA6B2)), center = p(0.5f, 0.58f), radius = u(0.3f)),
        p(0.23f, 0.39f), sz(0.54f, 0.34f),
    )
    if (active) {
        drawOval(WaterBlue.copy(alpha = 0.5f), p(0.29f, 0.45f), sz(0.42f, 0.22f))
        ripple(0.5f, 0.56f, phase, 0.12f)
    }
    drawCircle(Color(0xFF5B6673), u(0.025f), p(0.5f, 0.58f))
    // tap
    drawRoundRect(chrome(0.41f, 0.59f), p(0.41f, 0.17f), sz(0.18f, 0.11f), CornerRadius(u(0.04f)))
    drawRoundRect(chrome(0.47f, 0.53f), p(0.47f, 0.24f), sz(0.06f, 0.2f), CornerRadius(u(0.03f)))
    if (active) {
        drawRect(
            Brush.verticalGradient(listOf(WaterLight, WaterBlue), startY = u(0.43f), endY = u(0.56f)),
            p(0.485f, 0.43f), sz(0.03f, 0.13f),
        )
    }
}

fun DrawScope.drawWC(active: Boolean, phase: Float) {
    // cistern
    drawRoundRect(ceramic(0.1f, 0.3f), p(0.22f, 0.1f), sz(0.56f, 0.2f), CornerRadius(u(0.05f)))
    drawCircle(chrome(0.45f, 0.55f), u(0.045f), p(0.5f, 0.2f))
    // bowl
    drawOval(ceramic(0.28f, 0.9f), p(0.25f, 0.28f), sz(0.5f, 0.62f))
    drawOval(Color(0xFFDCE3EA), p(0.31f, 0.36f), sz(0.38f, 0.47f))
    drawOval(Color(0xFFB7C2CD), p(0.31f, 0.36f), sz(0.38f, 0.47f), style = Stroke(u(0.02f)))
    drawOval(
        if (active) WaterBlue.copy(alpha = 0.65f) else Color(0xFF9FD0EE).copy(alpha = 0.5f),
        p(0.37f, 0.48f), sz(0.26f, 0.28f),
    )
    if (active) {
        drawArc(
            Color.White.copy(alpha = 0.75f), startAngle = phase * 360f, sweepAngle = 200f, useCenter = false,
            topLeft = p(0.4f, 0.51f), size = sz(0.2f, 0.22f), style = Stroke(u(0.022f), cap = StrokeCap.Round),
        )
    }
}

fun DrawScope.drawShower(active: Boolean, phase: Float) {
    // tray
    drawRoundRect(ceramic(0.7f, 0.88f), p(0.14f, 0.7f), sz(0.72f, 0.17f), CornerRadius(u(0.04f)))
    drawCircle(Color(0xFF7B8794), u(0.025f), p(0.5f, 0.785f))
    // arm and head
    drawLine(ChromeMid, p(0.14f, 0.12f), p(0.5f, 0.12f), u(0.045f), cap = StrokeCap.Round)
    drawLine(ChromeMid, p(0.5f, 0.12f), p(0.5f, 0.19f), u(0.045f), cap = StrokeCap.Round)
    drawOval(chrome(0.31f, 0.69f), p(0.31f, 0.18f), sz(0.38f, 0.1f))
    for (k in 0 until 5) drawCircle(Color(0xFF4A5560), u(0.012f), p(0.38f + k * 0.06f, 0.235f))
    if (active) {
        for (k in 0 until 7) {
            val x = 0.35f + k * 0.05f
            for (m in 0 until 3) {
                val q = (phase + k * 0.13f + m / 3f) % 1f
                val y = 0.3f + q * 0.38f
                drawLine(WaterLight.copy(alpha = 0.85f), p(x, y), p(x, y + 0.05f), u(0.014f), cap = StrokeCap.Round)
            }
        }
        ripple(0.5f, 0.78f, phase, 0.1f)
    }
}

fun DrawScope.drawKitchen(active: Boolean, phase: Float) {
    val steel = Brush.verticalGradient(listOf(SteelLight, SteelMid), startY = u(0.36f), endY = u(0.84f))
    drawRoundRect(steel, p(0.1f, 0.36f), sz(0.8f, 0.48f), CornerRadius(u(0.06f)))
    for (x in listOf(0.16f, 0.52f)) {
        drawRoundRect(
            Brush.radialGradient(listOf(Color(0xFFB9C4CF), Color(0xFF6C7B8B)), center = p(x + 0.16f, 0.6f), radius = u(0.22f)),
            p(x, 0.42f), sz(0.32f, 0.36f), CornerRadius(u(0.05f)),
        )
        drawCircle(Color(0xFF4A5560), u(0.025f), p(x + 0.16f, 0.6f))
    }
    if (active) {
        drawRoundRect(WaterBlue.copy(alpha = 0.45f), p(0.55f, 0.5f), sz(0.26f, 0.22f), CornerRadius(u(0.05f)))
        ripple(0.68f, 0.6f, phase, 0.1f)
    }
    // gooseneck tap
    drawRoundRect(chrome(0.42f, 0.5f), p(0.42f, 0.24f), sz(0.08f, 0.14f), CornerRadius(u(0.02f)))
    drawArc(
        ChromeMid, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = p(0.46f, 0.1f), size = sz(0.22f, 0.3f), style = Stroke(u(0.045f), cap = StrokeCap.Round),
    )
    if (active) {
        drawRect(
            Brush.verticalGradient(listOf(WaterLight, WaterBlue), startY = u(0.26f), endY = u(0.58f)),
            p(0.665f, 0.26f), sz(0.03f, 0.32f),
        )
    }
}
