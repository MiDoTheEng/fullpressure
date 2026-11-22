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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate

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
/** Soft shadow: a few spreading layers, lighter at the edges, like a real blurred shadow. */
private fun DrawScope.shadowRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    for (k in 3 downTo 0) {
        val g = k * 0.012f
        drawRoundRect(Color.Black.copy(alpha = 0.07f + (3 - k) * 0.03f), p(x + 0.025f - g, y + 0.035f - g), sz(w + 2 * g, h + 2 * g), CornerRadius(u(r + g)))
    }
}

private fun DrawScope.shadowOval(x: Float, y: Float, w: Float, h: Float) {
    for (k in 3 downTo 0) {
        val g = k * 0.012f
        drawOval(Color.Black.copy(alpha = 0.07f + (3 - k) * 0.03f), p(x + 0.025f - g, y + 0.035f - g), sz(w + 2 * g, h + 2 * g))
    }
}

/** Soft round shadow under a circle centred at ([cx], [cy]). */
private fun DrawScope.shadowCircle(cx: Float, cy: Float, r: Float) {
    for (k in 3 downTo 0) {
        val g = k * 0.012f
        drawCircle(Color.Black.copy(alpha = 0.07f + (3 - k) * 0.03f), u(r + g), p(cx + 0.025f, cy + 0.035f))
    }
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
        Room.GARDEN -> {
            drawRect(Brush.linearGradient(
                listOf(Color(0xFF5E9E3A), Color(0xFF4A8A2E)),
                start = Offset.Zero, end = Offset(size.width, size.height),
            ))
            // grass blades
            for (k in 0 until 40) {
                val x = ((k * 37 + index * 13) % 100) / 100f
                val y = ((k * 53 + index * 29) % 100) / 100f
                val c = if (k % 3 == 0) Color(0x5582C95A) else if (k % 3 == 1) Color(0x44396E22) else Color(0x3399DD70)
                drawLine(c, p(x, y), p(x + ((k % 5) - 2) * 0.01f, y - 0.05f), u(0.012f), cap = StrokeCap.Round)
            }
        }
        Room.VILLA -> {
            // light oak parquet in 2x2 blocks, alternating direction
            val tones = listOf(Color(0xFFE2C49A), Color(0xFFD6B386), Color(0xFFE8CDA6))
            val half = size.width / 2f
            for (by in 0..1) for (bx in 0..1) {
                val vertical = (bx + by + index) % 2 == 0
                for (k in 0 until 3) {
                    val tone = tones[(k + bx + by * 2 + index) % 3]
                    val tl = if (vertical) Offset(bx * half + k * half / 3f, by * half) else Offset(bx * half, by * half + k * half / 3f)
                    val sz2 = if (vertical) Size(half / 3f, half) else Size(half, half / 3f)
                    drawRect(tone, tl, sz2)
                    drawRect(Color(0x339A7246), tl, sz2, style = Stroke(u(0.006f)))
                }
            }
        }
        Room.TOWER -> {
            // polished concrete with a window reflection
            drawRect(Brush.linearGradient(
                listOf(Color(0xFFB9BFC6), Color(0xFF9AA2AB)),
                start = Offset.Zero, end = Offset(size.width, size.height),
            ))
            for (k in 0 until 10) {
                val x = ((k * 41 + index * 17) % 100) / 100f
                val y = ((k * 67 + index * 7) % 100) / 100f
                drawCircle(Color(0x22000000), u(0.008f + (k % 3) * 0.004f), p(x, y))
            }
            drawLine(Color(0x33FFFFFF), p(0.0f, 0.75f), p(0.75f, 0.0f), u(0.12f))
        }
        Room.STREET -> {
            // interlocking paving stones
            drawRect(Color(0xFF8A8C90))
            val tones = listOf(Color(0xFFC9C3B6), Color(0xFFB8B2A5), Color(0xFFD4CEC2))
            val rows = 4
            val bh = size.height / rows
            val bw = size.width / 2f
            val gap = u(0.025f)
            for (r in 0 until rows) {
                var x = if (r % 2 == 0) 0f else -bw / 2f
                var k = 0
                while (x < size.width) {
                    drawRoundRect(tones[(r + k + index) % 3], Offset(x + gap / 2, r * bh + gap / 2), Size(bw - gap, bh - gap), CornerRadius(u(0.02f)))
                    x += bw; k++
                }
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
        Room.GARDEN -> {
            // clipped hedge
            drawRect(Color(0xFF1F4A1A))
            for (k in 0 until 14) {
                val x = 0.12f + ((k * 37 + index * 5) % 76) / 100f
                val y = 0.12f + ((k * 53 + index * 11) % 76) / 100f
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF5FA441), Color(0xFF2C6A22)), center = p(x - 0.04f, y - 0.04f), radius = u(0.2f)),
                    u(0.16f), p(x, y),
                )
            }
            for (k in 0 until 8) {
                val x = ((k * 29 + index * 3) % 100) / 100f
                val y = ((k * 71 + index * 13) % 100) / 100f
                drawCircle(Color(0x5599E070), u(0.02f), p(x, y))
            }
        }
        Room.VILLA -> {
            // wall in plan view: solid with hatching, like a drawing
            drawRect(Color(0xFFEDE6DA))
            drawRect(Color(0xFF6B6157), style = Stroke(u(0.03f)))
            var x = -1f
            while (x < 1f) {
                drawLine(Color(0x886B6157), p(x, 1f), p(x + 1f, 0f), u(0.015f))
                x += 0.16f
            }
        }
        Room.TOWER -> {
            // floor-to-ceiling glass with the city behind it
            drawRect(Brush.verticalGradient(listOf(Color(0xFF6FB6E8), Color(0xFFBEE3F7)), startY = 0f, endY = size.height))
            val heights = listOf(0.55f, 0.35f, 0.7f, 0.45f, 0.6f, 0.3f)
            for (k in 0 until 6) {
                val h = heights[(k + index) % heights.size]
                val x = k * 0.18f - 0.04f
                drawRect(Color(0xFF5C7D99), p(x, 1f - h), sz(0.15f, h))
                for (wy in 0 until 6) {
                    val y = 1f - h + 0.06f + wy * 0.09f
                    if (y < 0.97f) drawRect(Color(0x66FFF4C2), p(x + 0.03f, y), sz(0.03f, 0.03f))
                }
            }
            // mullions and glare
            drawRect(Color(0xFF39434E), style = Stroke(u(0.06f)))
            drawLine(Color(0xFF39434E), p(0.5f, 0f), p(0.5f, 1f), u(0.03f))
            drawLine(Color(0x55FFFFFF), p(0.1f, 0.9f), p(0.4f, 0.1f), u(0.05f))
        }
        Room.STREET -> {
            // a little parked car, seen from above
            drawRect(Color(0xFF55595E))
            drawLine(Color(0xFFF2F2F2), p(0.04f, 0f), p(0.04f, 1f), u(0.025f))
            val colors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFFC107), Color(0xFF43A047))
            val body = colors[index % colors.size]
            drawRoundRect(Color(0x44000000), p(0.25f, 0.1f), sz(0.54f, 0.84f), CornerRadius(u(0.14f)))
            drawRoundRect(Brush.horizontalGradient(listOf(body.copy(red = body.red * 0.7f, green = body.green * 0.7f, blue = body.blue * 0.7f), body, body.copy(red = body.red * 0.75f, green = body.green * 0.75f, blue = body.blue * 0.75f)), startX = u(0.22f), endX = u(0.78f)),
                p(0.22f, 0.07f), sz(0.56f, 0.84f), CornerRadius(u(0.14f)))
            // windscreen, roof, rear window
            drawRoundRect(Color(0xFF263238), p(0.29f, 0.24f), sz(0.42f, 0.14f), CornerRadius(u(0.04f)))
            drawRoundRect(body.copy(alpha = 1f), p(0.3f, 0.39f), sz(0.4f, 0.24f), CornerRadius(u(0.04f)))
            drawRoundRect(Color(0xFF263238), p(0.31f, 0.64f), sz(0.38f, 0.1f), CornerRadius(u(0.03f)))
            drawLine(Color(0x55FFFFFF), p(0.33f, 0.27f), p(0.45f, 0.27f), u(0.02f), cap = StrokeCap.Round)
            // headlights and wheels
            drawCircle(Color(0xFFFFF59D), u(0.035f), p(0.3f, 0.11f))
            drawCircle(Color(0xFFFFF59D), u(0.035f), p(0.7f, 0.11f))
            for ((wx, wy) in listOf(0.2f to 0.25f, 0.8f to 0.25f, 0.2f to 0.74f, 0.8f to 0.74f))
                drawRoundRect(Color(0xFF1C1C1C), p(wx - 0.03f, wy - 0.07f), sz(0.06f, 0.14f), CornerRadius(u(0.02f)))
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
    // printed manufacturer marking along the pipe
    val print = if (horizontal) Offset(0f, -t * 0.22f) else Offset(-t * 0.22f, 0f)
    drawLine(
        Color(0x40FFFFFF), armPoint(d, from) + print, armPoint(d, to) + print, u(0.006f),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(u(0.025f), u(0.012f), u(0.008f), u(0.03f))),
    )
}

private fun DrawScope.armShadow(d: Dir, from: Float, to: Float) {
    val (tl, s) = armRect(d, from, to, u(PIPE_THICKNESS), shift = p(0.03f, 0.04f))
    drawRect(Shadow, tl, s)
}

/**
 * Realistic water along [path]: dark body, bright core, bubbles carried along, foamy front.
 * [core] lets the fast-moving core and bubbles take a different line (the outside of a bend).
 */
private fun DrawScope.waterPath(path: Path, flow: Float, front: Boolean, core: Path = path) {
    val t = u(PIPE_THICKNESS)
    val measure = PathMeasure()
    measure.setPath(core, false)
    val len = measure.length
    if (len < 0.5f) return
    drawPath(path, WaterDeep, style = Stroke(t * 0.56f, cap = StrokeCap.Butt))
    drawPath(path, WaterBlue, style = Stroke(t * 0.42f, cap = StrokeCap.Butt))
    drawPath(core, WaterLight.copy(alpha = 0.55f), style = Stroke(t * 0.14f, cap = StrokeCap.Butt))
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
            val lane = ((k * 7) % 5 - 2) * t * 0.05f
            val off = Offset(-tan.y * lane, tan.x * lane)
            val r = t * (0.035f + (k % 3) * 0.02f)
            drawCircle(Color.White.copy(alpha = 0.75f), r, pos + off)
        }
        k++
    }
    if (front) {
        // the advancing front: rounded, foamy, with spray just ahead of it
        val end = measure.getPosition(len)
        val tan = measure.getTangent(len)
        drawCircle(WaterBlue, t * 0.24f, end)
        drawCircle(Color.White.copy(alpha = 0.55f + 0.3f * flow), t * 0.16f, end)
        for (k2 in 0 until 3) {
            val q = (flow + k2 / 3f) % 1f
            val side = (k2 - 1) * t * 0.12f
            val pos = end + tan * (t * (0.1f + q * 0.22f)) + Offset(-tan.y * side, tan.x * side)
            drawCircle(WaterLight.copy(alpha = 1f - q), t * 0.05f, pos)
        }
    }
}

/** Swirling water where flows meet or split (tees and crosses). */
private fun DrawScope.turbulence(flow: Float) {
    val t = u(PIPE_THICKNESS)
    drawCircle(WaterBlue, t * 0.34f, center)
    for (k in 0 until 3) {
        drawArc(
            Color.White.copy(alpha = 0.6f), flow * 360f * 1.5f + k * 120f, 70f, false,
            Offset(center.x - t * 0.22f, center.y - t * 0.22f), Size(t * 0.44f, t * 0.44f),
            style = Stroke(t * 0.06f, cap = StrokeCap.Round),
        )
    }
    for (k in 0 until 4) {
        val a = (flow * 2f + k / 4f) * 6.2832f
        drawCircle(Color.White.copy(alpha = 0.7f), t * 0.04f,
            Offset(center.x + kotlin.math.cos(a) * t * 0.14f, center.y + kotlin.math.sin(a) * t * 0.14f))
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
    // moulded ridges on the socket
    for (f in listOf(0.88f, 0.94f)) {
        val m = armPoint(d, f)
        drawLine(Color(0x33000000), m - edge * 0.92f, m + edge * 0.92f, u(0.01f))
        val hl = if (horizontal) Offset(u(0.008f), 0f) else Offset(0f, u(0.008f))
        drawLine(Color(0x33FFFFFF), m - edge * 0.92f + hl, m + edge * 0.92f + hl, u(0.006f))
    }
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
        // momentum throws the fast water and bubbles to the outside of the bend
        val ro = half + t * 0.1f
        val outer = Path().apply {
            arcTo(Rect(Offset(corner.x - ro, corner.y - ro), Size(2 * ro, 2 * ro)), start, sweep, true)
        }
        waterPath(path, flow, fill < 1f, outer)
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
            if (second > 0f) for (d in open) if (d.bit != inletBit) {
                // water keeps going straight first; side branches fill a moment later
                val straightOn = d.bit == opposite(inletBit)
                val prog = if (straightOn || open.size == 2) second else ((second - 0.2f) / 0.8f).coerceIn(0f, 1f)
                if (prog > 0f) waterLine(armPoint(d, 0f), armPoint(d, prog), flow, front = prog < 1f)
            }
            if (open.size >= 3 && fill >= 0.45f) turbulence(flow)
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
 * Rooftop polyethylene water tank (side view) on a concrete base.
 * The body is translucent so you can see the water level, which drops by [drain] 0..1.
 * Outlet pipes leave the tank wall through brass tank connectors on each of [sides],
 * centred so they meet the neighbouring pipe exactly; water runs out along them by [fill].
 */
fun DrawScope.drawTank(phase: Float, drain: Float, sides: Int, fill: Float, flow: Float) {
    // concrete base
    drawOval(Color(0x2E000000), p(0.14f, 0.87f), sz(0.76f, 0.1f))
    drawRoundRect(Color(0xFFA9A39A), p(0.16f, 0.84f), sz(0.68f, 0.09f), CornerRadius(u(0.015f)))
    drawRect(Color(0xFFC9C3B8), p(0.16f, 0.84f), sz(0.68f, 0.025f))
    // body: domed top, straight sides
    val body = Path().apply {
        moveTo(u(0.2f), u(0.3f))
        quadraticBezierTo(u(0.2f), u(0.14f), u(0.5f), u(0.13f))
        quadraticBezierTo(u(0.8f), u(0.14f), u(0.8f), u(0.3f))
        lineTo(u(0.8f), u(0.82f))
        quadraticBezierTo(u(0.8f), u(0.86f), u(0.76f), u(0.86f))
        lineTo(u(0.24f), u(0.86f))
        quadraticBezierTo(u(0.2f), u(0.86f), u(0.2f), u(0.82f))
        close()
    }
    drawPath(body, Brush.horizontalGradient(
        listOf(Color(0xFF8C969C), Color(0xFFE9ECEA), Color(0xFFFFFFFF), Color(0xFFD3D8D6), Color(0xFF7E888E)),
        startX = u(0.2f), endX = u(0.8f)))
    // water seen through the translucent wall, with a gently moving surface
    val level = 0.46f + drain * 0.2f
    val water = Path().apply {
        moveTo(u(0.21f), u(level))
        for (k in 0..12) {
            val x = 0.21f + 0.58f * k / 12f
            lineTo(u(x), u(level + 0.008f * kotlin.math.sin(k * 1.1f + phase * 6.2832f)))
        }
        lineTo(u(0.79f), u(0.82f))
        quadraticBezierTo(u(0.79f), u(0.85f), u(0.76f), u(0.85f))
        lineTo(u(0.24f), u(0.85f))
        quadraticBezierTo(u(0.21f), u(0.85f), u(0.21f), u(0.82f))
        close()
    }
    drawPath(water, Brush.horizontalGradient(
        listOf(Color(0x8C5E8FB0), Color(0x59A9D4F0), Color(0x8C4F7E9E)), startX = u(0.2f), endX = u(0.8f)))
    // moulded ribs
    for (y in listOf(0.34f, 0.47f, 0.6f, 0.73f)) {
        drawLine(Color(0x2E000000), p(0.2f, y), p(0.8f, y), u(0.016f), cap = StrokeCap.Round)
        drawLine(Color(0x99FFFFFF), p(0.2f, y - 0.015f), p(0.8f, y - 0.015f), u(0.01f), cap = StrokeCap.Round)
    }
    // maker's label
    drawRoundRect(Color(0xFF1E5FA8), p(0.38f, 0.62f), sz(0.24f, 0.07f), CornerRadius(u(0.01f)))
    drawRect(Color.White, p(0.41f, 0.64f), sz(0.13f, 0.012f))
    drawRect(Color(0xCCFFFFFF), p(0.41f, 0.662f), sz(0.09f, 0.01f))
    // screw lid
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF0D3B66), Color(0xFF3D8BD0), Color(0xFF0D3B66)), startX = u(0.38f), endX = u(0.62f)),
        p(0.38f, 0.09f), sz(0.24f, 0.06f), CornerRadius(u(0.02f)))
    var gx = 0.41f
    while (gx < 0.6f) { drawLine(Color(0xFF0A2A4A), p(gx, 0.095f), p(gx, 0.145f), u(0.006f)); gx += 0.03f }
    // sheen
    drawLine(Color(0x80FFFFFF), p(0.3f, 0.22f), p(0.3f, 0.8f), u(0.02f), cap = StrokeCap.Round)

    // outlet pipes through brass tank connectors, out to the cell edge
    for (d in DIRS) {
        if (sides and d.bit == 0) continue
        val from = when (d.bit) { E, W -> 0.6f; N -> 0.74f; else -> 0.72f }
        armShadow(d, from, 1f)
        pipeArm(d, from, 1f)
        if (fill > 0f) waterLine(armPoint(d, from), armPoint(d, from + (1f - from) * fill), flow, front = fill < 1f)
        socket(d)
        // brass tank connector (bulkhead fitting) at the tank wall
        val t = u(PIPE_THICKNESS) * 1.2f
        val horizontal = d.dr == 0
        val (tl, sz2) = armRect(d, from - 0.04f, from + 0.06f, t)
        val lo = if (horizontal) tl.y else tl.x
        drawRect(across(listOf(Color(0xFF6E5012), Color(0xFFF0CF77), Color(0xFFB38B2C), Color(0xFF6E5012)), horizontal, lo, lo + t), tl, sz2)
        drawRect(Color(0xFF4A350A), tl, sz2, style = Stroke(u(0.008f)))
    }
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
    // soap pump on the left corner
    shadowCircle(0.21f, 0.27f, 0.05f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB3C7), Color(0xFFE91E63)), center = p(0.2f, 0.25f), radius = u(0.07f)), u(0.05f), p(0.21f, 0.27f))
    drawCircle(chrome(0.19f, 0.23f), u(0.022f), p(0.21f, 0.27f))
    drawLine(ChromeMid, p(0.21f, 0.27f), p(0.27f, 0.27f), u(0.014f), cap = StrokeCap.Round)
    // toothbrush cup on the right corner
    shadowCircle(0.79f, 0.27f, 0.05f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB2EBF2), Color(0xFF26A69A)), center = p(0.78f, 0.25f), radius = u(0.07f)), u(0.05f), p(0.79f, 0.27f))
    drawLine(Color(0xFF42A5F5), p(0.79f, 0.27f), p(0.86f, 0.2f), u(0.016f), cap = StrokeCap.Round)
    drawLine(Color(0xFFFFCA28), p(0.79f, 0.27f), p(0.73f, 0.19f), u(0.016f), cap = StrokeCap.Round)
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
    // shampoo bottle and soap bar in the corner
    shadowRect(0.7f, 0.66f, 0.1f, 0.14f, 0.02f)
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFBA68C8), Color(0xFF6A1B9A)), startX = u(0.7f), endX = u(0.8f)), p(0.7f, 0.66f), sz(0.1f, 0.14f), CornerRadius(u(0.025f)))
    drawRoundRect(Color(0xFFEEEEEE), p(0.72f, 0.62f), sz(0.06f, 0.05f), CornerRadius(u(0.01f)))
    shadowRect(0.18f, 0.7f, 0.12f, 0.08f, 0.03f)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFBC02D)), startY = u(0.7f), endY = u(0.78f)), p(0.18f, 0.7f), sz(0.12f, 0.08f), CornerRadius(u(0.03f)))
    if (active) for (k in 0 until 3) drawCircle(Color.White.copy(alpha = 0.85f), u(0.012f + k * 0.006f), p(0.21f + k * 0.035f, 0.69f - (k % 2) * 0.02f))
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
        // bubble-bath foam along the sides
        for (k in 0 until 14) {
            val x = 0.22f + (k % 7) * 0.09f
            val y = if (k < 7) 0.29f else 0.71f
            val wob = 0.006f * kotlin.math.sin(phase * 6.2832f + k)
            drawCircle(Color.White.copy(alpha = 0.9f), u(0.03f + (k % 3) * 0.012f + wob), p(x, y))
            drawCircle(Color(0x33A6E3FF), u(0.012f), p(x - 0.008f, y - 0.008f))
        }
    }
    drawCircle(Color(0xFF5B6673), u(0.022f), p(0.72f, 0.5f))
    // rubber duck: sits in the empty tub, floats and bobs when it fills
    val dx = if (active) 0.04f * kotlin.math.sin(phase * 6.2832f) else 0f
    val dy = if (active) 0.012f * kotlin.math.sin(phase * 12.566f) else 0f
    drawRubberDuck(0.56f + dx, 0.52f + dy, if (active) 6f * kotlin.math.sin(phase * 6.2832f) else 0f)
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
    drawCircle(Color(0xFF4A5560), u(0.025f), p(0.62f, 0.55f))
    if (active) {
        drawRoundRect(WaterBlue.copy(alpha = 0.45f), p(0.47f, 0.34f), sz(0.3f, 0.37f), CornerRadius(u(0.05f)))
        ripple(0.62f, 0.52f, phase, 0.1f)
    }
    // fruit waiting to be washed; it bobs and shines when the water runs
    fun bob(k: Int) = if (active) 0.012f * kotlin.math.sin(phase * 6.2832f + k * 1.7f) else 0f
    drawApple(0.52f, 0.41f + bob(0))
    drawOrange(0.72f, 0.42f + bob(1))
    drawLemon(0.54f, 0.64f + bob(2))
    drawStrawberry(0.71f, 0.64f + bob(3))
    if (active) {
        for (k in 0 until 6) {
            val q = (phase * 2f + k / 6f) % 1f
            val (x, y) = listOf(0.5f to 0.38f, 0.74f to 0.39f, 0.56f to 0.61f, 0.69f to 0.61f, 0.55f to 0.44f, 0.7f to 0.46f)[k]
            drawCircle(Color.White.copy(alpha = 1f - q), u(0.008f + q * 0.012f), p(x, y))
        }
    }
    // washing-up liquid and a sponge on the drainer
    shadowRect(0.19f, 0.32f, 0.1f, 0.18f, 0.03f)
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF81C784), Color(0xFF2E7D32)), startX = u(0.19f), endX = u(0.29f)), p(0.19f, 0.32f), sz(0.1f, 0.18f), CornerRadius(u(0.03f)))
    drawRoundRect(Color(0xFFFFFFFF), p(0.215f, 0.28f), sz(0.05f, 0.05f), CornerRadius(u(0.01f)))
    drawRoundRect(Color(0xFFFFF59D), p(0.205f, 0.38f), sz(0.07f, 0.06f), CornerRadius(u(0.01f)))
    shadowRect(0.18f, 0.56f, 0.14f, 0.1f, 0.02f)
    drawRoundRect(Color(0xFFFFEB3B), p(0.18f, 0.56f), sz(0.14f, 0.1f), CornerRadius(u(0.02f)))
    drawRoundRect(Color(0xFF43A047), p(0.18f, 0.56f), sz(0.14f, 0.035f), CornerRadius(u(0.015f)))
    // pull-down tap from behind
    drawCircle(chrome(0.56f, 0.68f), u(0.05f), p(0.62f, 0.2f))
    drawLine(ChromeMid, p(0.62f, 0.2f), p(0.62f, 0.4f), u(0.035f), cap = StrokeCap.Round)
    if (active) stream(0.62f, 0.4f, 0.5f)
}

private fun DrawScope.drawRubberDuck(x: Float, y: Float, tilt: Float) {
    rotate(tilt, p(x, y)) {
        drawOval(Color(0x33000000), p(x - 0.11f + 0.015f, y - 0.06f + 0.02f), sz(0.22f, 0.14f))
        // body
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFF176), Color(0xFFFFD600), Color(0xFFF9A825)), center = p(x - 0.03f, y - 0.03f), radius = u(0.15f)),
            p(x - 0.11f, y - 0.06f), sz(0.22f, 0.14f))
        // tail
        val tail = Path().apply {
            moveTo(u(x + 0.09f), u(y - 0.03f)); lineTo(u(x + 0.14f), u(y - 0.07f)); lineTo(u(x + 0.11f), u(y + 0.01f)); close()
        }
        drawPath(tail, Color(0xFFFFD600))
        // wing
        drawOval(Color(0xFFF9A825), p(x - 0.01f, y - 0.03f), sz(0.09f, 0.05f))
        // head
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD600)), center = p(x - 0.09f, y - 0.1f), radius = u(0.08f)), u(0.06f), p(x - 0.07f, y - 0.07f))
        // beak
        drawOval(Color(0xFFFF7043), p(x - 0.165f, y - 0.075f), sz(0.06f, 0.03f))
        // eye
        drawCircle(Color(0xFF212121), u(0.012f), p(x - 0.085f, y - 0.085f))
        drawCircle(Color.White, u(0.004f), p(x - 0.088f, y - 0.088f))
    }
}

private fun DrawScope.drawApple(x: Float, y: Float) {
    drawCircle(Color(0x33000000), u(0.07f), p(x + 0.01f, y + 0.015f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF6B6B), Color(0xFFD32F2F), Color(0xFF8E1B1B)), center = p(x - 0.02f, y - 0.02f), radius = u(0.09f)), u(0.065f), p(x, y))
    drawLine(Color(0xFF5D4037), p(x, y - 0.055f), p(x + 0.01f, y - 0.085f), u(0.012f), cap = StrokeCap.Round)
    drawOval(Color(0xFF43A047), p(x + 0.012f, y - 0.09f), sz(0.045f, 0.022f))
    drawCircle(Color(0xAAFFFFFF), u(0.014f), p(x - 0.025f, y - 0.02f))
}

private fun DrawScope.drawOrange(x: Float, y: Float) {
    drawCircle(Color(0x33000000), u(0.068f), p(x + 0.01f, y + 0.015f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFCC80), Color(0xFFFF9800), Color(0xFFE65100)), center = p(x - 0.02f, y - 0.02f), radius = u(0.09f)), u(0.063f), p(x, y))
    for (k in 0 until 6) {
        val a = k * 1.047f
        drawCircle(Color(0x33A84300), u(0.005f), p(x + 0.035f * kotlin.math.cos(a), y + 0.035f * kotlin.math.sin(a)))
    }
    drawCircle(Color(0xFF7CB342), u(0.01f), p(x, y - 0.045f))
}

private fun DrawScope.drawLemon(x: Float, y: Float) {
    drawOval(Color(0x33000000), p(x - 0.065f + 0.01f, y - 0.045f + 0.015f), sz(0.13f, 0.09f))
    drawOval(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFDD835), Color(0xFFC6A700)), center = p(x - 0.02f, y - 0.015f), radius = u(0.09f)), p(x - 0.065f, y - 0.045f), sz(0.13f, 0.09f))
    drawCircle(Color(0xFFFDD835), u(0.012f), p(x - 0.07f, y))
    drawCircle(Color(0xFFFDD835), u(0.012f), p(x + 0.07f, y))
    drawCircle(Color(0x99FFFFFF), u(0.012f), p(x - 0.025f, y - 0.018f))
}

private fun DrawScope.drawStrawberry(x: Float, y: Float) {
    val berry = Path().apply {
        moveTo(u(x - 0.055f), u(y - 0.035f))
        quadraticBezierTo(u(x), u(y - 0.06f), u(x + 0.055f), u(y - 0.035f))
        quadraticBezierTo(u(x + 0.05f), u(y + 0.04f), u(x), u(y + 0.07f))
        quadraticBezierTo(u(x - 0.05f), u(y + 0.04f), u(x - 0.055f), u(y - 0.035f))
        close()
    }
    drawPath(berry, Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFC62828)), center = p(x - 0.015f, y - 0.01f), radius = u(0.08f)))
    for (k in 0 until 7) {
        val sx = x - 0.03f + (k % 3) * 0.03f
        val sy = y - 0.015f + (k / 3) * 0.025f
        drawCircle(Color(0xFFFFE082), u(0.005f), p(sx, sy))
    }
    for (k in -1..1) drawLine(Color(0xFF2E7D32), p(x, y - 0.045f), p(x + k * 0.03f, y - 0.065f), u(0.014f), cap = StrokeCap.Round)
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
    val c = p(0.5f, 0.57f)
    val r = u(0.2f)
    drawCircle(chrome(0.24f, 0.76f), u(0.25f), c)
    drawCircle(Color(0xFF1C2A38), r, c)
    clipPath(Path().apply { addOval(Rect(c, r)) }) {
        // drum holes
        for (k in 0 until 12) {
            val a = k * 0.5236f
            drawCircle(Color(0xFF2C3E50), u(0.012f), Offset(c.x + kotlin.math.cos(a) * r * 0.85f, c.y + kotlin.math.sin(a) * r * 0.85f))
        }
        if (active) {
            // soapy water sloshing in the bottom of the drum
            val wave = Path().apply {
                moveTo(c.x - r, c.y + r)
                lineTo(c.x - r, c.y + r * 0.1f)
                for (k in 0..12) {
                    val x = c.x - r + 2 * r * k / 12f
                    lineTo(x, c.y + r * 0.1f + r * 0.08f * kotlin.math.sin(k * 0.9f + phase * 12.566f))
                }
                lineTo(c.x + r, c.y + r)
                close()
            }
            drawPath(wave, WaterBlue.copy(alpha = 0.75f))
        }
        // clothes tumble round with the drum; when it's still they lie at the bottom
        rotate(if (active) phase * 720f else 0f, c) {
            // red T-shirt
            val shirt = Path().apply {
                val x = c.x - r * 0.45f
                val y = c.y + r * 0.25f
                val w = r * 0.42f
                moveTo(x - w * 0.5f, y - w * 0.45f)
                lineTo(x - w * 0.95f, y - w * 0.15f)
                lineTo(x - w * 0.75f, y + w * 0.1f)
                lineTo(x - w * 0.5f, y)
                lineTo(x - w * 0.5f, y + w * 0.6f)
                lineTo(x + w * 0.5f, y + w * 0.6f)
                lineTo(x + w * 0.5f, y)
                lineTo(x + w * 0.75f, y + w * 0.1f)
                lineTo(x + w * 0.95f, y - w * 0.15f)
                lineTo(x + w * 0.5f, y - w * 0.45f)
                lineTo(x + w * 0.15f, y - w * 0.45f)
                quadraticBezierTo(x, y - w * 0.25f, x - w * 0.15f, y - w * 0.45f)
                close()
            }
            drawPath(shirt, Color(0xFFE53935))
            drawPath(shirt, Color(0xFF8E1B1B), style = Stroke(u(0.006f)))
            // blue sock with a white heel
            drawRoundRect(Color(0xFF1E88E5), Offset(c.x + r * 0.05f, c.y + r * 0.45f), Size(r * 0.55f, r * 0.2f), CornerRadius(r * 0.1f))
            drawRoundRect(Color(0xFF1E88E5), Offset(c.x + r * 0.45f, c.y + r * 0.2f), Size(r * 0.2f, r * 0.4f), CornerRadius(r * 0.1f))
            drawCircle(Color.White, r * 0.08f, Offset(c.x + r * 0.12f, c.y + r * 0.55f))
            // yellow towel with stripes
            drawRoundRect(Color(0xFFFFC93C), Offset(c.x - r * 0.15f, c.y + r * 0.55f), Size(r * 0.5f, r * 0.3f), CornerRadius(r * 0.05f))
            drawLine(Color(0xFFFF8A1F), Offset(c.x - r * 0.15f, c.y + r * 0.65f), Offset(c.x + r * 0.35f, c.y + r * 0.65f), r * 0.05f)
            drawLine(Color(0xFFFF8A1F), Offset(c.x - r * 0.15f, c.y + r * 0.76f), Offset(c.x + r * 0.35f, c.y + r * 0.76f), r * 0.05f)
            // green shorts
            drawRoundRect(Color(0xFF43A047), Offset(c.x + r * 0.3f, c.y - r * 0.15f), Size(r * 0.35f, r * 0.25f), CornerRadius(r * 0.05f))
            drawLine(Color(0xFF1B5E20), Offset(c.x + r * 0.475f, c.y - r * 0.05f), Offset(c.x + r * 0.475f, c.y + r * 0.1f), r * 0.04f)
        }
        if (active) {
            // foam
            for (k in 0 until 7) {
                val q = (phase * 2f + k / 7f) % 1f
                drawCircle(Color.White.copy(alpha = 0.85f - q * 0.5f), r * (0.06f + (k % 3) * 0.03f),
                    Offset(c.x - r * 0.8f + k * r * 0.27f, c.y + r * 0.1f - q * r * 0.15f))
            }
        }
    }
    drawCircle(Color(0xFF8E99A5), r, c, style = Stroke(u(0.015f)))
    drawArc(Color(0x55FFFFFF), 200f, 70f, false, p(0.33f, 0.4f), sz(0.34f, 0.34f), style = Stroke(u(0.02f), cap = StrokeCap.Round))
}

// ---------- garden ----------

/** Brass garden tap on a stone post, filling a bucket. */
fun DrawScope.drawGardenTap(active: Boolean, phase: Float) {
    // stone post
    shadowRect(0.32f, 0.1f, 0.36f, 0.3f, 0.04f)
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFFBDB6A8), Color(0xFF8C8577)), start = p(0.32f, 0.1f), end = p(0.68f, 0.4f)),
        p(0.32f, 0.1f), sz(0.36f, 0.3f), CornerRadius(u(0.04f)),
    )
    // brass bib tap
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFF0CF77), Color(0xFF8C6A1E)), center = p(0.48f, 0.26f), radius = u(0.08f)),
        u(0.06f), p(0.5f, 0.28f),
    )
    drawRoundRect(Color(0xFFB38B2C), p(0.47f, 0.28f), sz(0.06f, 0.14f), CornerRadius(u(0.02f)))
    drawLine(Color(0xFFD23B3B), p(0.42f, 0.24f), p(0.58f, 0.24f), u(0.035f), cap = StrokeCap.Round)
    // metal bucket
    shadowOval(0.27f, 0.5f, 0.46f, 0.4f)
    drawOval(
        Brush.horizontalGradient(listOf(Color(0xFF7D8894), Color(0xFFE3E8ED), Color(0xFF8E99A5)), startX = u(0.27f), endX = u(0.73f)),
        p(0.27f, 0.5f), sz(0.46f, 0.4f),
    )
    drawOval(Color(0xFF4F5B66), p(0.31f, 0.54f), sz(0.38f, 0.32f))
    if (active) {
        drawOval(WaterBlue, p(0.33f, 0.56f), sz(0.34f, 0.28f))
        drawCircle(Color.White.copy(alpha = (1f - phase) * 0.8f), u(0.03f + phase * 0.12f), p(0.5f, 0.7f), style = Stroke(u(0.012f)))
        drawRect(
            Brush.horizontalGradient(listOf(WaterBlue, WaterLight, WaterBlue), startX = u(0.485f), endX = u(0.515f)),
            p(0.485f, 0.42f), sz(0.03f, 0.28f),
        )
    }
}

/** Pop-up sprinkler that sweeps a jet around and wets the lawn. */
fun DrawScope.drawSprinkler(active: Boolean, phase: Float) {
    val c = p(0.5f, 0.5f)
    if (active) {
        drawCircle(Color(0x55234A14), u(0.44f), c)
        val a = phase * 6.2832f * 2f
        for (k in 0 until 12) {
            val q = k / 12f
            val ang = a - q * 0.6f
            val r = u(0.08f + q * 0.36f)
            drawCircle(WaterLight.copy(alpha = 0.95f - q * 0.5f), u(0.022f - q * 0.008f),
                Offset(c.x + kotlin.math.cos(ang) * r, c.y + kotlin.math.sin(ang) * r))
        }
        for (k in 0 until 6) {
            val ang = a + 3.1416f + k * 0.12f
            val r = u(0.2f + (k % 3) * 0.08f)
            drawCircle(WaterLight.copy(alpha = 0.6f), u(0.014f), Offset(c.x + kotlin.math.cos(ang) * r, c.y + kotlin.math.sin(ang) * r))
        }
    }
    drawCircle(Shadow, u(0.12f), c + p(0.02f, 0.03f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF4B5560), Color(0xFF1E252C)), center = c, radius = u(0.13f)), u(0.12f), c)
    drawCircle(Color(0xFFE2B93B), u(0.05f), c)
    drawCircle(Color(0xFF1E252C), u(0.02f), c)
}

/** Round stone fountain with a jet in the middle. */
fun DrawScope.drawFountain(active: Boolean, phase: Float) {
    val c = p(0.5f, 0.5f)
    drawCircle(Shadow, u(0.42f), c + p(0.03f, 0.04f))
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFE0D9CB), Color(0xFFA39A88)), center = p(0.45f, 0.45f), radius = u(0.45f)),
        u(0.42f), c,
    )
    drawCircle(
        if (active) Brush.radialGradient(listOf(WaterLight, WaterBlue, WaterDeep), center = c, radius = u(0.34f))
        else Brush.radialGradient(listOf(Color(0xFF6E8796), Color(0xFF4A5D6A)), center = c, radius = u(0.34f)),
        u(0.33f), c,
    )
    // wishing coins at the bottom
    for ((cx, cy) in listOf(0.34f to 0.42f, 0.62f to 0.36f, 0.66f to 0.62f, 0.4f to 0.64f, 0.56f to 0.7f)) {
        drawCircle(Color(0xFFC9A227).copy(alpha = if (active) 0.7f else 0.9f), u(0.025f), p(cx, cy))
        drawCircle(Color(0x66FFF3B0), u(0.01f), p(cx - 0.006f, cy - 0.006f))
    }
    drawCircle(Color(0xFFC9C1B2), u(0.08f), c)
    if (active) {
        for (k in 0 until 3) {
            val q = (phase + k / 3f) % 1f
            drawCircle(Color.White.copy(alpha = (1f - q) * 0.7f), u(0.1f + q * 0.22f), c, style = Stroke(u(0.01f)))
        }
        // droplets thrown up and falling back around the jet
        for (k in 0 until 10) {
            val q = (phase * 1.5f + k / 10f) % 1f
            val ang = k * 0.628f
            val r = u(0.04f + q * 0.2f)
            val lift = u(0.12f) * kotlin.math.sin(q * 3.1416f)
            drawCircle(WaterLight, u(0.018f),
                Offset(c.x + kotlin.math.cos(ang) * r, c.y + kotlin.math.sin(ang) * r - lift))
        }
        drawCircle(Color.White, u(0.035f), c - p(0f, 0.05f))
    }
}


// ---------- street ----------

/** Red fire hydrant; when it gets water it sprays jets out of both side nozzles. */
fun DrawScope.drawHydrant(active: Boolean, phase: Float) {
    if (active) {
        drawOval(WaterBlue.copy(alpha = 0.35f), p(0.08f, 0.72f), sz(0.84f, 0.2f))
        // arcing jets from both nozzles
        for (side in listOf(-1f, 1f)) {
            for (k in 0 until 9) {
                val q = (phase + k / 9f) % 1f
                val x = 0.5f + side * (0.18f + q * 0.3f)
                val y = 0.44f - 0.12f * kotlin.math.sin(q * 3.1416f) + q * 0.3f
                drawCircle(WaterLight.copy(alpha = 1f - q * 0.6f), u(0.03f - q * 0.012f), p(x, y))
            }
        }
    }
    // base
    drawRoundRect(Color(0x44000000), p(0.32f, 0.8f), sz(0.4f, 0.08f), CornerRadius(u(0.03f)))
    drawRoundRect(Color(0xFFB71C1C), p(0.3f, 0.74f), sz(0.4f, 0.1f), CornerRadius(u(0.03f)))
    // barrel
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF8E1212), Color(0xFFFF6B6B), Color(0xFFE53935), Color(0xFF8E1212)), startX = u(0.34f), endX = u(0.66f)),
        p(0.34f, 0.32f), sz(0.32f, 0.44f), CornerRadius(u(0.04f)),
    )
    // dome cap
    drawArc(
        Brush.horizontalGradient(listOf(Color(0xFF8E1212), Color(0xFFFF6B6B), Color(0xFF8E1212)), startX = u(0.32f), endX = u(0.68f)),
        180f, 180f, true, p(0.32f, 0.18f), sz(0.36f, 0.28f),
    )
    drawRoundRect(Color(0xFFFFC107), p(0.45f, 0.12f), sz(0.1f, 0.08f), CornerRadius(u(0.02f)))
    drawRoundRect(Color(0xFFB71C1C), p(0.31f, 0.3f), sz(0.38f, 0.05f), CornerRadius(u(0.02f)))
    // side nozzles with yellow caps
    for (side in listOf(-1f, 1f)) {
        drawRoundRect(Color(0xFFC62828), p(0.5f + side * 0.2f - 0.06f, 0.4f), sz(0.12f, 0.1f), CornerRadius(u(0.02f)))
        if (!active) drawCircle(Color(0xFFFFC107), u(0.035f), p(0.5f + side * 0.26f, 0.45f))
    }
    // front nozzle
    drawCircle(Color(0xFFC62828), u(0.06f), p(0.5f, 0.55f))
    drawCircle(Color(0xFFFFC107), u(0.035f), p(0.5f, 0.55f))
}

/** Stone drinking fountain with a little bird that dips its head to drink. */
fun DrawScope.drawDrinkingFountain(active: Boolean, phase: Float) {
    // pedestal
    drawRoundRect(Color(0x44000000), p(0.36f, 0.48f), sz(0.32f, 0.42f), CornerRadius(u(0.03f)))
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF8C8577), Color(0xFFD9D2C4), Color(0xFF9E978A)), startX = u(0.34f), endX = u(0.66f)),
        p(0.34f, 0.46f), sz(0.32f, 0.42f), CornerRadius(u(0.03f)),
    )
    // bowl
    drawOval(Color(0x44000000), p(0.18f, 0.38f), sz(0.68f, 0.18f))
    drawOval(
        Brush.horizontalGradient(listOf(Color(0xFF8C8577), Color(0xFFE6E0D3), Color(0xFF9E978A)), startX = u(0.16f), endX = u(0.84f)),
        p(0.16f, 0.34f), sz(0.68f, 0.18f),
    )
    drawOval(if (active) WaterBlue.copy(alpha = 0.8f) else Color(0xFF6E7D88), p(0.22f, 0.36f), sz(0.56f, 0.12f))
    // bubbler and its little arc of water
    drawRoundRect(chrome(0.47f, 0.53f), p(0.47f, 0.3f), sz(0.06f, 0.08f), CornerRadius(u(0.02f)))
    if (active) {
        val arc = Path().apply {
            moveTo(u(0.5f), u(0.3f))
            quadraticBezierTo(u(0.58f), u(0.12f), u(0.66f), u(0.38f))
        }
        drawPath(arc, WaterLight, style = Stroke(u(0.025f), cap = StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = 1f - phase), u(0.01f + phase * 0.04f), p(0.66f, 0.41f), style = Stroke(u(0.008f)))
    }
    // the bird on the rim: dips to drink when there's water
    val dip = if (active) (kotlin.math.sin(phase * 6.2832f).coerceAtLeast(0f)) * 25f else 0f
    val bx = 0.28f
    val by = 0.33f
    rotate(dip, p(bx, by + 0.04f)) {
        drawOval(Color(0xFF42A5F5), p(bx - 0.08f, by - 0.05f), sz(0.14f, 0.1f))      // body
        drawOval(Color(0xFF1E88E5), p(bx - 0.1f, by - 0.035f), sz(0.07f, 0.05f))      // tail
        drawOval(Color(0xFF90CAF9), p(bx - 0.03f, by - 0.01f), sz(0.07f, 0.05f))      // belly
        drawCircle(Color(0xFF42A5F5), u(0.045f), p(bx + 0.06f, by - 0.07f))           // head
        drawCircle(Color.White, u(0.014f), p(bx + 0.07f, by - 0.08f))
        drawCircle(Color(0xFF212121), u(0.008f), p(bx + 0.074f, by - 0.08f))
        val beak = Path().apply {
            moveTo(u(bx + 0.1f), u(by - 0.08f)); lineTo(u(bx + 0.14f), u(by - 0.065f)); lineTo(u(bx + 0.1f), u(by - 0.055f)); close()
        }
        drawPath(beak, Color(0xFFFFA000))
    }
    drawLine(Color(0xFFFFA000), p(bx - 0.01f, by + 0.04f), p(bx - 0.01f, by + 0.06f), u(0.01f))
    drawLine(Color(0xFFFFA000), p(bx + 0.02f, by + 0.04f), p(bx + 0.02f, by + 0.06f), u(0.01f))
}
