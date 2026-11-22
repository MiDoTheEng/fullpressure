package com.midotheeng.fullpressure

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// ---------- palette ----------
val WaterBlue = Color(0xFF3FA9F5)
private val WaterDeep = Color(0xFF1668B8)
private val WaterLight = Color(0xFFA6E3FF)

// Green PPR / PVC pipe
private val PvcDark = Color(0xFF14592A)
private val PvcMid = Color(0xFF2A8C45)
private val PvcLight = Color(0xFF8FE0A6)
private val PvcSocketDark = Color(0xFF0F4A22)
private val PvcSocketMid = Color(0xFF237A3B)

private val ChromeDark = Color(0xFF6F7B88)
private val ChromeLight = Color(0xFFF4F7FA)
private val ChromeMid = Color(0xFFA4AFBA)
private val CeramicWhite = Color(0xFFFFFFFF)
private val CeramicShade = Color(0xFFCBD4DD)
private val Outline = Color(0xFF7D8996)
private val SteelDark = Color(0xFF5E6E80)
private val SteelLight = Color(0xFFE1E7ED)
private val SteelMid = Color(0xFF9DABBA)
private val Shadow = Color(0x40000000)

private val pvcAcross = listOf(PvcDark, PvcMid, PvcLight, PvcMid, PvcDark)
private val socketAcross = listOf(PvcSocketDark, PvcSocketMid, PvcLight.copy(alpha = 0.9f), PvcSocketMid, PvcSocketDark)

const val PIPE_THICKNESS = 0.30f

private fun DrawScope.u(f: Float) = f * size.minDimension
private fun DrawScope.p(x: Float, y: Float) = Offset(u(x), u(y))
private fun DrawScope.sz(w: Float, h: Float) = Size(u(w), u(h))

private fun across(colors: List<Color>, horizontal: Boolean, a: Float, b: Float): Brush =
    if (horizontal) Brush.verticalGradient(colors, startY = a, endY = b)
    else Brush.horizontalGradient(colors, startX = a, endX = b)

/** Soft drop shadow for a rounded rectangle. */
private fun DrawScope.shadowRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    drawRoundRect(Shadow, p(x + 0.03f, y + 0.04f), sz(w, h), CornerRadius(u(r)))
}

private fun DrawScope.shadowOval(x: Float, y: Float, w: Float, h: Float) {
    drawOval(Shadow, p(x + 0.03f, y + 0.04f), sz(w, h))
}

// ---------- rooms ----------

/** Bathroom: glazed porcelain floor tile. Kitchen: oak planks. */
fun DrawScope.drawFloor(room: Room, index: Int) {
    when (room) {
        Room.BATHROOM -> {
            drawRect(Brush.linearGradient(
                listOf(Color(0xFFE6ECF1), Color(0xFFC9D3DC)),
                start = Offset.Zero, end = Offset(size.width, size.height),
            ))
            // faint marble veins, different per tile
            val v = (index * 37 % 10) / 10f
            drawLine(Color(0x1A5B6B7B), p(0.1f, 0.2f + v * 0.5f), p(0.9f, 0.35f + v * 0.3f), u(0.01f))
            drawLine(Color(0x125B6B7B), p(0.3f + v * 0.3f, 0.05f), p(0.55f, 0.95f), u(0.008f))
            drawLine(Color(0x55FFFFFF), p(0.08f, 0.08f), p(0.4f, 0.08f), u(0.02f), cap = StrokeCap.Round)
        }
        Room.KITCHEN -> {
            val tones = listOf(Color(0xFFC28A55), Color(0xFFB57C48), Color(0xFFCB955F))
            val plank = size.height / 3f
            for (k in 0 until 3) {
                val tone = tones[(k + index) % 3]
                drawRect(
                    Brush.verticalGradient(listOf(tone, tone.copy(red = tone.red * 0.92f, green = tone.green * 0.9f)),
                        startY = k * plank, endY = (k + 1) * plank),
                    topLeft = Offset(0f, k * plank), size = Size(size.width, plank),
                )
                // grain
                for (g in 0 until 3) {
                    val y = k * plank + plank * (0.25f + g * 0.25f)
                    drawLine(Color(0x22603A16), Offset(0f, y), Offset(size.width, y + u(0.02f) * (g - 1)), u(0.008f))
                }
                drawLine(Color(0x55452710), Offset(0f, (k + 1) * plank), Offset(size.width, (k + 1) * plank), u(0.012f))
                // plank end joint, staggered
                val jx = u(((index * 3 + k * 5) % 7) / 7f)
                drawLine(Color(0x55452710), Offset(jx, k * plank), Offset(jx, (k + 1) * plank), u(0.012f))
            }
        }
    }
}

/** Bathroom: mosaic-tiled wall. Kitchen: granite countertop. */
fun DrawScope.drawWall(room: Room, index: Int) {
    when (room) {
        Room.BATHROOM -> {
            drawRect(Color(0xFFF2F5F7))
            val n = 4
            val cell = size.width / n
            val gap = u(0.02f)
            val tones = listOf(Color(0xFF2E7F99), Color(0xFF3C95B0), Color(0xFF5BAEC6), Color(0xFF266E86))
            for (r in 0 until n) for (c in 0 until n) {
                val tone = tones[(r * 3 + c * 5 + index) % tones.size]
                drawRoundRect(
                    Brush.linearGradient(listOf(tone.copy(alpha = 1f), tone.copy(red = tone.red * 0.85f)),
                        start = Offset(c * cell, r * cell), end = Offset((c + 1) * cell, (r + 1) * cell)),
                    topLeft = Offset(c * cell + gap / 2, r * cell + gap / 2),
                    size = Size(cell - gap, cell - gap), cornerRadius = CornerRadius(u(0.02f)),
                )
            }
        }
        Room.KITCHEN -> {
            drawRect(Brush.linearGradient(
                listOf(Color(0xFF3A3F45), Color(0xFF24282D)),
                start = Offset.Zero, end = Offset(size.width, size.height),
            ))
            // speckles
            for (k in 0 until 26) {
                val x = ((k * 37 + index * 11) % 100) / 100f
                val y = ((k * 61 + index * 7) % 100) / 100f
                val c = if (k % 3 == 0) Color(0x66FFFFFF) else if (k % 3 == 1) Color(0x44C8A878) else Color(0x55000000)
                drawCircle(c, u(0.012f + (k % 4) * 0.004f), p(x, y))
            }
            drawLine(Color(0x33FFFFFF), p(0.06f, 0.08f), p(0.5f, 0.08f), u(0.02f), cap = StrokeCap.Round)
        }
    }
}

// ---------- pipes ----------

private fun DrawScope.armRect(d: Dir, from: Float, to: Float, thick: Float, shift: Offset = Offset.Zero): Pair<Offset, Size> {
    val half = size.minDimension / 2f
    val c = center + shift
    val a = Offset(c.x + d.dc * from * half, c.y + d.dr * from * half)
    val b = Offset(c.x + d.dc * to * half, c.y + d.dr * to * half)
    return if (d.dr == 0) {
        Offset(minOf(a.x, b.x), c.y - thick / 2) to Size(kotlin.math.abs(b.x - a.x), thick)
    } else {
        Offset(c.x - thick / 2, minOf(a.y, b.y)) to Size(thick, kotlin.math.abs(b.y - a.y))
    }
}

private fun DrawScope.armPoint(d: Dir, f: Float): Offset {
    val half = size.minDimension / 2f
    return Offset(center.x + d.dc * f * half, center.y + d.dr * f * half)
}

/** Straight run of green pipe from [from] to [to] (0 = centre, 1 = cell edge). */
private fun DrawScope.pipeArm(d: Dir, from: Float, to: Float) {
    val t = u(PIPE_THICKNESS)
    val horizontal = d.dr == 0
    val (tl, s) = armRect(d, from, to, t)
    val lo = if (horizontal) tl.y else tl.x
    drawRect(across(pvcAcross, horizontal, lo, lo + t), tl, s)
    val side = if (horizontal) Offset(0f, t * 0.28f) else Offset(t * 0.28f, 0f)
    drawLine(Color(0x33FFFFFF), armPoint(d, from) + side, armPoint(d, to) + side, u(0.008f))
}

private fun DrawScope.armShadow(d: Dir, from: Float, to: Float) {
    val (tl, s) = armRect(d, from, to, u(PIPE_THICKNESS), shift = p(0.03f, 0.04f))
    drawRect(Shadow, tl, s)
}

/** Realistic water along [path]: dark body, bright core, bubbles carried along, foamy front. */
private fun DrawScope.waterPath(path: Path, flow: Float, front: Boolean) {
    val t = u(PIPE_THICKNESS)
    val measure = PathMeasure()
    measure.setPath(path, false)
    val len = measure.length
    if (len < 0.5f) return
    drawPath(path, WaterDeep, style = Stroke(t * 0.56f, cap = StrokeCap.Butt))
    drawPath(path, WaterBlue, style = Stroke(t * 0.42f, cap = StrokeCap.Butt))
    drawPath(path, WaterLight.copy(alpha = 0.55f), style = Stroke(t * 0.14f, cap = StrokeCap.Butt))
    // bubbles drifting with the flow, at different sizes and lanes
    val count = 8
    val spacing = u(0.09f)
    val period = count * spacing
    var k = 0
    while (k < count) {
        val dist = (flow * period + k * spacing) % period
        if (dist <= len) {
            val pos = measure.getPosition(dist)
            val tan = measure.getTangent(dist)
            val lane = ((k * 7) % 5 - 2) * t * 0.06f
            val off = Offset(-tan.y * lane, tan.x * lane)
            val r = t * (0.035f + (k % 3) * 0.02f)
            drawCircle(Color.White.copy(alpha = 0.75f), r, pos + off)
        }
        k++
    }
    if (front) {
        // the advancing water front: rounded, with foam
        val end = measure.getPosition(len)
        drawCircle(WaterBlue, t * 0.24f, end)
        drawCircle(Color.White.copy(alpha = 0.55f + 0.3f * flow), t * 0.16f, end)
        drawCircle(WaterLight, t * 0.08f, end)
    }
}

/** Water inside a straight run, flowing from [a] to [b]. */
private fun DrawScope.waterLine(a: Offset, b: Offset, flow: Float, front: Boolean = false) {
    if ((a - b).getDistance() < 0.5f) return
    val path = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y) }
    waterPath(path, flow, front)
}

/** Socket (coupling) where the pipe meets the cell edge. */
private fun DrawScope.socket(d: Dir) {
    val t = u(PIPE_THICKNESS) * 1.28f
    val horizontal = d.dr == 0
    val (tl, s) = armRect(d, 0.82f, 1f, t)
    val lo = if (horizontal) tl.y else tl.x
    drawRect(across(socketAcross, horizontal, lo, lo + t), tl, s)
    val rim = armPoint(d, 0.82f)
    val edge = if (horizontal) Offset(0f, t / 2) else Offset(t / 2, 0f)
    drawLine(Color(0x66000000), rim - edge, rim + edge, u(0.014f))
}

private fun angleOf(dx: Float, dy: Float): Float =
    (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f

/** Smooth 90° bend between openings [a] and [b], with water entering from [inletBit]. */
private fun DrawScope.bend(a: Dir, b: Dir, fill: Float, inletBit: Int, flow: Float) {
    val half = size.minDimension / 2f
    val t = u(PIPE_THICKNESS)
    val corner = Offset(center.x + (a.dc + b.dc) * half, center.y + (a.dr + b.dr) * half)
    val tl = Offset(corner.x - half, corner.y - half)
    val box = Size(2 * half, 2 * half)
    // point on edge A is at direction -B from the corner, and vice versa
    val angA = angleOf(-b.dc.toFloat(), -b.dr.toFloat())
    val angB = angleOf(-a.dc.toFloat(), -a.dr.toFloat())
    val delta = ((angB - angA + 540f) % 360f) - 180f

    drawArc(Shadow, angA, delta, false, tl + p(0.03f, 0.04f), box, style = Stroke(t))
    val r0 = (half - t / 2) / (half + t / 2)
    val band = 1f - r0
    drawArc(
        Brush.radialGradient(
            0f to PvcDark, r0 to PvcDark, r0 + band * 0.3f to PvcMid, r0 + band * 0.55f to PvcLight,
            r0 + band * 0.8f to PvcMid, 1f to PvcDark,
            center = corner, radius = half + t / 2,
        ),
        angA, delta, false, tl, box, style = Stroke(t),
    )
    if (fill > 0f) {
        val fromA = inletBit == a.bit
        val start = if (fromA) angA else angB
        val sweep = (if (fromA) delta else -delta) * fill
        val path = Path().apply { arcTo(Rect(tl, box), start, sweep, true) }
        waterPath(path, flow, fill < 1f)
    }
}

/**
 * A pipe piece with openings [mask] in its current orientation.
 * [fill] 0..1 is how far the water has got through it, entering from side [inletBit].
 */
fun DrawScope.drawPipe(mask: Int, fill: Float, inletBit: Int, flow: Float) {
    val open = DIRS.filter { mask and it.bit != 0 }
    val elbow = open.size == 2 && mask != (N or S) && mask != (E or W)
    if (elbow) {
        bend(open[0], open[1], fill, inletBit, flow)
    } else {
        for (d in open) armShadow(d, 0f, 1f)
        for (d in open) pipeArm(d, 0f, 1f)
        if (fill > 0f) {
            val inArm = open.firstOrNull { it.bit == inletBit }
            val first = (fill * 2f).coerceAtMost(1f)
            val second = (fill * 2f - 1f).coerceIn(0f, 1f)
            if (inArm != null) waterLine(armPoint(inArm, 1f), armPoint(inArm, 1f - first), flow, front = second == 0f && fill < 1f)
            if (second > 0f) for (d in open) if (d.bit != inletBit) waterLine(armPoint(d, 0f), armPoint(d, second), flow, front = fill < 1f)
        }
    }
    for (d in open) socket(d)
}

/**
 * Pipe stubs from a tank/fixture body out to the connected [sides].
 * Water shows in the [wetSides] stubs; [fill] runs it inward from the edge (fixtures) or it's full (tank).
 */
fun DrawScope.drawStubs(sides: Int, wetSides: Int, fill: Float, flow: Float, outward: Boolean = false) {
    for (d in DIRS) if (sides and d.bit != 0) {
        armShadow(d, 0.4f, 1f)
        pipeArm(d, 0.4f, 1f)
        if (wetSides and d.bit != 0 && fill > 0f) {
            if (outward) waterLine(armPoint(d, 0.4f), armPoint(d, 0.4f + 0.6f * fill), flow, front = fill < 1f)
            else waterLine(armPoint(d, 1f), armPoint(d, 1f - 0.6f * fill), flow, front = fill < 1f)
        }
        socket(d)
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
        drawOval(WaterBlue.copy(alpha = 0.4f), Offset(e.x - u(0.22f), e.y - u(0.13f)), sz(0.44f, 0.26f))
        for (k in 0 until 10) {
            val q = (phase + k / 10f) % 1f
            val spread = ((k % 5) - 2f) / 2f
            val pos = e + back * (q * u(0.28f)) + perp * (spread * q * u(0.34f))
            drawCircle(WaterLight.copy(alpha = (1f - q) * 0.95f), u(0.035f) * (1f - q * 0.5f), pos)
        }
    }
}

// ---------- main water tank ----------

/**
 * Water storage tank, side view, with a cut-away window showing the water.
 * [drain] 0..1 lowers the level a little as water leaves.
 */
fun DrawScope.drawTank(phase: Float, drain: Float) {
    val left = 0.2f
    val right = 0.8f
    val top = 0.16f
    val bottom = 0.8f
    // stand
    drawRect(Color(0xFF3B4650), p(0.24f, 0.8f), sz(0.06f, 0.12f))
    drawRect(Color(0xFF3B4650), p(0.7f, 0.8f), sz(0.06f, 0.12f))
    drawRect(Color(0xFF55616C), p(0.2f, 0.78f), sz(0.6f, 0.05f))
    // shadow
    drawRoundRect(Shadow, p(left + 0.03f, top + 0.03f), sz(right - left, bottom - top), CornerRadius(u(0.08f)))
    // body (blue polyethylene)
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF16406E), Color(0xFF3F8BD0), Color(0xFF2A6AAE), Color(0xFF123659)),
            startX = u(left), endX = u(right)),
        p(left, top), sz(right - left, bottom - top), CornerRadius(u(0.08f)),
    )
    // domed top and lid
    drawOval(
        Brush.horizontalGradient(listOf(Color(0xFF1B4C80), Color(0xFF5FA4E0), Color(0xFF1B4C80)), startX = u(left), endX = u(right)),
        p(left, top - 0.06f), sz(right - left, 0.14f),
    )
    drawRoundRect(Color(0xFF0F2E52), p(0.4f, top - 0.1f), sz(0.2f, 0.06f), CornerRadius(u(0.02f)))
    // cut-away window showing the water inside
    val wl = 0.29f
    val wr = 0.71f
    val wt = 0.24f
    val wb = 0.74f
    drawRoundRect(Color(0xFF0C1B2C), p(wl, wt), sz(wr - wl, wb - wt), CornerRadius(u(0.04f)))
    val level = 0.38f + drain * 0.12f
    val amp = u(0.012f)
    val wave = Path().apply {
        moveTo(u(wl), u(wb))
        lineTo(u(wl), u(level))
        val steps = 16
        for (k in 0..steps) {
            val x = wl + (wr - wl) * k / steps
            val y = u(level) + amp * kotlin.math.sin((k / steps.toFloat()) * 12.566f + phase * 6.2832f)
            lineTo(u(x), y)
        }
        lineTo(u(wr), u(wb))
        close()
    }
    drawPath(wave, Brush.verticalGradient(listOf(WaterLight, WaterBlue, WaterDeep), startY = u(level), endY = u(wb)))
    // rising bubbles
    for (k in 0 until 5) {
        val q = (phase + k * 0.21f) % 1f
        val x = wl + 0.06f + k * 0.075f
        val y = wb - 0.04f - q * (wb - level - 0.06f)
        drawCircle(Color.White.copy(alpha = 0.6f * (1f - q)), u(0.012f + (k % 2) * 0.006f), p(x, y))
    }
    drawRoundRect(Color(0x66FFFFFF), p(wl, wt), sz(wr - wl, wb - wt), CornerRadius(u(0.04f)), style = Stroke(u(0.012f)))
    // ribs
    for (y in listOf(0.2f, 0.76f)) drawLine(Color(0x44000000), p(left, y), p(right, y), u(0.018f))
    // glass glare
    drawLine(Color(0x33FFFFFF), p(wl + 0.04f, wt + 0.03f), p(wl + 0.04f, wb - 0.03f), u(0.02f), cap = StrokeCap.Round)
}

// ---------- fixtures ----------

private fun DrawScope.chrome(x0: Float, x1: Float) =
    Brush.horizontalGradient(listOf(ChromeDark, ChromeLight, ChromeMid), startX = u(x0), endX = u(x1))

private fun DrawScope.ceramic(y0: Float, y1: Float) =
    Brush.verticalGradient(listOf(CeramicWhite, CeramicShade), startY = u(y0), endY = u(y1))

private fun DrawScope.ripple(cx: Float, cy: Float, phase: Float, max: Float) {
    drawCircle(Color.White.copy(alpha = (1f - phase) * 0.8f), u(0.03f + phase * max), p(cx, cy), style = Stroke(u(0.012f)))
    val q = (phase + 0.5f) % 1f
    drawCircle(Color.White.copy(alpha = (1f - q) * 0.5f), u(0.03f + q * max), p(cx, cy), style = Stroke(u(0.01f)))
}

private fun DrawScope.stream(x: Float, y0: Float, y1: Float) {
    drawRect(
        Brush.horizontalGradient(listOf(WaterBlue, WaterLight, WaterBlue), startX = u(x - 0.018f), endX = u(x + 0.018f)),
        p(x - 0.018f, y0), sz(0.036f, y1 - y0),
    )
}

/** Vanity basin, seen from above. */
fun DrawScope.drawBasin(active: Boolean, phase: Float) {
    shadowRect(0.12f, 0.16f, 0.76f, 0.68f, 0.06f)
    // marble vanity top
    drawRoundRect(ceramic(0.16f, 0.84f), p(0.12f, 0.16f), sz(0.76f, 0.68f), CornerRadius(u(0.06f)))
    drawLine(Color(0x1A5B6B7B), p(0.16f, 0.7f), p(0.84f, 0.6f), u(0.01f))
    drawRoundRect(Outline, p(0.12f, 0.16f), sz(0.76f, 0.68f), CornerRadius(u(0.06f)), style = Stroke(u(0.012f)))
    // bowl
    drawOval(
        Brush.radialGradient(listOf(Color(0xFFF7F9FB), Color(0xFFB4BFCA)), center = p(0.5f, 0.6f), radius = u(0.28f)),
        p(0.24f, 0.38f), sz(0.52f, 0.38f),
    )
    drawOval(Outline, p(0.24f, 0.38f), sz(0.52f, 0.38f), style = Stroke(u(0.012f)))
    if (active) {
        drawOval(WaterBlue.copy(alpha = 0.45f), p(0.3f, 0.47f), sz(0.4f, 0.24f))
        ripple(0.5f, 0.6f, phase, 0.12f)
    }
    drawCircle(Color(0xFF5B6673), u(0.022f), p(0.5f, 0.6f))
    // mixer tap
    drawCircle(chrome(0.43f, 0.57f), u(0.065f), p(0.5f, 0.27f))
    drawRoundRect(chrome(0.47f, 0.53f), p(0.47f, 0.27f), sz(0.06f, 0.17f), CornerRadius(u(0.03f)))
    drawLine(ChromeDark, p(0.5f, 0.27f), p(0.63f, 0.22f), u(0.03f), cap = StrokeCap.Round)
    if (active) stream(0.5f, 0.43f, 0.58f)
}

/** Walk-in shower tray with a ceiling rain head, seen from above. */
fun DrawScope.drawShower(active: Boolean, phase: Float) {
    shadowRect(0.1f, 0.1f, 0.8f, 0.8f, 0.05f)
    drawRoundRect(ceramic(0.1f, 0.9f), p(0.1f, 0.1f), sz(0.8f, 0.8f), CornerRadius(u(0.05f)))
    // anti-slip grooves
    for (k in 1 until 6) {
        val y = 0.1f + k * 0.8f / 6f
        drawLine(Color(0x22000000), p(0.16f, y), p(0.84f, y), u(0.008f))
    }
    drawRoundRect(Outline, p(0.1f, 0.1f), sz(0.8f, 0.8f), CornerRadius(u(0.05f)), style = Stroke(u(0.012f)))
    // glass screen on one side
    drawLine(Color(0xAA9FD8F0), p(0.1f, 0.12f), p(0.1f, 0.88f), u(0.03f))
    // linear drain
    drawRoundRect(SteelMid, p(0.3f, 0.78f), sz(0.4f, 0.05f), CornerRadius(u(0.02f)))
    if (active) {
        drawRoundRect(WaterBlue.copy(alpha = 0.18f), p(0.12f, 0.12f), sz(0.76f, 0.76f), CornerRadius(u(0.05f)))
        // drops landing all over the tray
        for (k in 0 until 9) {
            val q = (phase + k * 0.11f) % 1f
            val x = 0.25f + ((k * 37) % 50) / 100f
            val y = 0.3f + ((k * 23) % 45) / 100f
            drawCircle(Color.White.copy(alpha = (1f - q) * 0.8f), u(0.01f + q * 0.05f), p(x, y), style = Stroke(u(0.008f)))
        }
    }
    // rain shower head (round, chrome, with nozzles)
    drawCircle(Color(0x33000000), u(0.17f), p(0.53f, 0.45f))
    drawCircle(
        Brush.radialGradient(listOf(ChromeLight, ChromeMid, ChromeDark), center = p(0.46f, 0.38f), radius = u(0.22f)),
        u(0.16f), p(0.5f, 0.42f),
    )
    for (ring in 1..2) for (k in 0 until ring * 6) {
        val a = (k.toFloat() / (ring * 6)) * 6.2832f
        val r = ring * 0.055f
        drawCircle(Color(0xFF55606C), u(0.009f), p(0.5f + r * kotlin.math.cos(a), 0.42f + r * kotlin.math.sin(a)))
    }
    drawCircle(Color(0xFF55606C), u(0.009f), p(0.5f, 0.42f))
}

/** Bathtub, seen from above. */
fun DrawScope.drawBathtub(active: Boolean, phase: Float) {
    shadowRect(0.08f, 0.14f, 0.84f, 0.72f, 0.16f)
    drawRoundRect(ceramic(0.14f, 0.86f), p(0.08f, 0.14f), sz(0.84f, 0.72f), CornerRadius(u(0.16f)))
    drawRoundRect(Outline, p(0.08f, 0.14f), sz(0.84f, 0.72f), CornerRadius(u(0.16f)), style = Stroke(u(0.012f)))
    drawRoundRect(
        Brush.radialGradient(listOf(Color(0xFFF7F9FB), Color(0xFFB8C3CE)), center = p(0.55f, 0.5f), radius = u(0.4f)),
        p(0.16f, 0.22f), sz(0.68f, 0.56f), CornerRadius(u(0.24f)),
    )
    if (active) {
        drawRoundRect(
            Brush.radialGradient(listOf(WaterLight.copy(alpha = 0.7f), WaterBlue.copy(alpha = 0.7f)), center = p(0.5f, 0.5f), radius = u(0.4f)),
            p(0.19f, 0.25f), sz(0.62f, 0.5f), CornerRadius(u(0.22f)),
        )
        ripple(0.3f, 0.5f, phase, 0.16f)
    }
    drawCircle(Color(0xFF5B6673), u(0.022f), p(0.72f, 0.5f))
    // tap at the end
    drawRoundRect(chrome(0.1f, 0.2f), p(0.11f, 0.44f), sz(0.08f, 0.12f), CornerRadius(u(0.03f)))
    drawLine(ChromeMid, p(0.17f, 0.5f), p(0.27f, 0.5f), u(0.035f), cap = StrokeCap.Round)
    if (active) drawCircle(WaterLight, u(0.025f), p(0.29f, 0.5f))
}

/** Stainless kitchen sink set into a counter, seen from above. */
fun DrawScope.drawKitchenSink(active: Boolean, phase: Float) {
    shadowRect(0.08f, 0.14f, 0.84f, 0.72f, 0.04f)
    // granite counter piece
    drawRoundRect(Color(0xFF2E3338), p(0.08f, 0.14f), sz(0.84f, 0.72f), CornerRadius(u(0.04f)))
    // steel sink with drainer grooves on the left
    drawRoundRect(
        Brush.verticalGradient(listOf(SteelLight, SteelMid), startY = u(0.24f), endY = u(0.8f)),
        p(0.14f, 0.24f), sz(0.72f, 0.56f), CornerRadius(u(0.05f)),
    )
    for (k in 0 until 5) drawLine(Color(0x33000000), p(0.19f + k * 0.045f, 0.32f), p(0.19f + k * 0.045f, 0.72f), u(0.012f))
    drawRoundRect(
        Brush.radialGradient(listOf(Color(0xFFC9D2DB), Color(0xFF6C7B8B)), center = p(0.62f, 0.53f), radius = u(0.26f)),
        p(0.44f, 0.3f), sz(0.36f, 0.44f), CornerRadius(u(0.06f)),
    )
    if (active) {
        drawRoundRect(WaterBlue.copy(alpha = 0.45f), p(0.47f, 0.4f), sz(0.3f, 0.3f), CornerRadius(u(0.05f)))
        ripple(0.62f, 0.55f, phase, 0.1f)
    }
    drawCircle(Color(0xFF4A5560), u(0.025f), p(0.62f, 0.55f))
    // pull-down tap from behind
    drawCircle(chrome(0.56f, 0.68f), u(0.05f), p(0.62f, 0.2f))
    drawLine(ChromeMid, p(0.62f, 0.2f), p(0.62f, 0.42f), u(0.035f), cap = StrokeCap.Round)
    if (active) stream(0.62f, 0.42f, 0.55f)
}

/** Dishwasher, front panel. */
fun DrawScope.drawDishwasher(active: Boolean, phase: Float) {
    shadowRect(0.12f, 0.1f, 0.76f, 0.8f, 0.05f)
    drawRoundRect(
        Brush.horizontalGradient(listOf(SteelMid, SteelLight, SteelMid, SteelDark), startX = u(0.12f), endX = u(0.88f)),
        p(0.12f, 0.1f), sz(0.76f, 0.8f), CornerRadius(u(0.05f)),
    )
    // control strip
    drawRoundRect(Color(0xFF1E252C), p(0.12f, 0.1f), sz(0.76f, 0.14f), CornerRadius(u(0.05f)))
    for (k in 0 until 4) {
        val on = active && ((phase * 4).toInt() % 4 >= k)
        drawCircle(if (on) Color(0xFF3DD68C) else Color(0xFF4A5560), u(0.018f), p(0.22f + k * 0.08f, 0.17f))
    }
    drawRoundRect(if (active) Color(0xFF7FD4FF) else Color(0xFF34404C), p(0.6f, 0.13f), sz(0.2f, 0.08f), CornerRadius(u(0.015f)))
    // handle
    drawRoundRect(chrome(0.25f, 0.75f), p(0.25f, 0.3f), sz(0.5f, 0.045f), CornerRadius(u(0.02f)))
    drawLine(Color(0x22000000), p(0.14f, 0.38f), p(0.86f, 0.38f), u(0.01f))
    if (active) {
        // steam wisps
        for (k in 0 until 3) {
            val q = (phase + k / 3f) % 1f
            drawCircle(Color.White.copy(alpha = (1f - q) * 0.5f), u(0.03f + q * 0.04f), p(0.35f + k * 0.15f, 0.08f - q * 0.06f + 0.06f))
        }
    }
}

/** Front-loading washing machine. */
fun DrawScope.drawWasher(active: Boolean, phase: Float) {
    shadowRect(0.12f, 0.08f, 0.76f, 0.84f, 0.06f)
    drawRoundRect(ceramic(0.08f, 0.92f), p(0.12f, 0.08f), sz(0.76f, 0.84f), CornerRadius(u(0.06f)))
    drawRoundRect(Outline, p(0.12f, 0.08f), sz(0.76f, 0.84f), CornerRadius(u(0.06f)), style = Stroke(u(0.012f)))
    // control panel
    drawLine(Color(0x33000000), p(0.12f, 0.24f), p(0.88f, 0.24f), u(0.01f))
    drawCircle(chrome(0.66f, 0.78f), u(0.05f), p(0.72f, 0.16f))
    drawRoundRect(if (active) Color(0xFF3DD68C) else Color(0xFF4A5560), p(0.2f, 0.13f), sz(0.18f, 0.06f), CornerRadius(u(0.015f)))
    // door
    drawCircle(chrome(0.24f, 0.76f), u(0.25f), p(0.5f, 0.57f))
    drawCircle(Color(0xFF1C2A38), u(0.2f), p(0.5f, 0.57f))
    if (active) {
        drawCircle(WaterBlue.copy(alpha = 0.7f), u(0.19f), p(0.5f, 0.6f))
        drawArc(
            Color.White.copy(alpha = 0.8f), startAngle = phase * 360f, sweepAngle = 220f, useCenter = false,
            topLeft = p(0.37f, 0.44f), size = sz(0.26f, 0.26f), style = Stroke(u(0.03f), cap = StrokeCap.Round),
        )
        drawArc(
            WaterLight.copy(alpha = 0.8f), startAngle = phase * 360f + 180f, sweepAngle = 140f, useCenter = false,
            topLeft = p(0.42f, 0.49f), size = sz(0.16f, 0.16f), style = Stroke(u(0.02f), cap = StrokeCap.Round),
        )
    }
    drawArc(Color(0x55FFFFFF), 200f, 70f, false, p(0.33f, 0.4f), sz(0.34f, 0.34f), style = Stroke(u(0.02f), cap = StrokeCap.Round))
}
