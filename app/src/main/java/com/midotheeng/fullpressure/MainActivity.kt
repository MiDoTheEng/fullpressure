package com.midotheeng.fullpressure

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0E1A2B)
private val TileBg = Color(0xFF16263B)
private val PipeOuter = Color(0xFF5B6B80)
private val PipeEmpty = Color(0xFF22344C)
private val Water = WaterBlue
private val Leak = Color(0xFFE5484D)
private val Ok = Color(0xFF3DD68C)
private val Gold = Color(0xFFF5C451)
private val WallC = Color(0xFF2B3746)
private val TankC = Color(0xFF2F6FB0)
private val FixtureBg = Color(0xFF24364F)
private val TextDim = Color(0xFF9AA8BA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Water, background = Bg)) {
                App()
            }
        }
    }
}

private fun prefs(ctx: Context) = ctx.getSharedPreferences("progress", Context.MODE_PRIVATE)
private fun loadStars(ctx: Context, n: Int) = prefs(ctx).getInt("stars_$n", 0)
private fun saveStars(ctx: Context, n: Int, stars: Int) {
    if (stars > loadStars(ctx, n)) prefs(ctx).edit().putInt("stars_$n", stars).apply()
}

@Composable
fun App() {
    var current by remember { mutableStateOf<Int?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        val n = current
        if (n == null) {
            LevelSelect(refresh) { current = it }
        } else {
            BackHandler { current = null; refresh++ }
            GameScreen(
                level = LEVELS[n - 1],
                onBack = { current = null; refresh++ },
                onNext = { current = if (n < LEVELS.size) n + 1 else null; refresh++ },
            )
        }
    }
}

@Composable
fun LevelSelect(refresh: Int, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    val stars = remember(refresh) { LEVELS.map { loadStars(ctx, it.number) } }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("FULL PRESSURE", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("Get water to every fixture", color = TextDim, fontSize = 15.sp)
        Spacer(Modifier.height(36.dp))
        Text("World 1 · Villa", color = Water, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LEVELS.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { level ->
                    val i = level.number - 1
                    val unlocked = i == 0 || stars[i - 1] > 0
                    Column(
                        Modifier
                            .size(56.dp, 72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (unlocked) FixtureBg else WallC)
                            .clickable(enabled = unlocked) { onPick(level.number) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (unlocked) "${level.number}" else "–",
                            color = if (unlocked) Color.White else TextDim,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(starText(stars[i]), color = Gold, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun starText(n: Int) = "★".repeat(n) + "☆".repeat(3 - n)

private enum class Phase { PLAYING, FLOWING, DONE }

@Composable
fun GameScreen(level: Level, onBack: () -> Unit, onNext: () -> Unit) {
    val ctx = LocalContext.current
    val puzzle = remember(level.number) { makePuzzle(level) }
    val rots = remember(level.number) { mutableStateListOf<Int>().apply { addAll(puzzle.start) } }
    var moves by remember(level.number) { mutableIntStateOf(0) }
    var phase by remember(level.number) { mutableStateOf(Phase.PLAYING) }
    var result by remember(level.number) { mutableStateOf<FlowResult?>(null) }
    var revealed by remember(level.number) { mutableIntStateOf(0) }
    var timeLeft by remember(level.number) { mutableIntStateOf(level.timeLimit ?: 0) }
    var timedOut by remember(level.number) { mutableStateOf(false) }
    var attempt by remember(level.number) { mutableIntStateOf(0) }
    var stars by remember(level.number) { mutableIntStateOf(0) }

    fun restart() {
        rots.clear(); rots.addAll(puzzle.start)
        moves = 0; result = null; revealed = 0; timedOut = false
        timeLeft = level.timeLimit ?: 0
        phase = Phase.PLAYING
        attempt++
    }

    // Countdown timer
    LaunchedEffect(level.number, attempt) {
        if (level.timeLimit == null) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            if (phase == Phase.PLAYING) timeLeft--
        }
        if (phase == Phase.PLAYING) {
            timedOut = true
            phase = Phase.DONE
        }
    }

    // Water flow animation, then the verdict
    LaunchedEffect(phase) {
        val r = result
        if (phase == Phase.FLOWING && r != null) {
            for (k in 1..r.order.size) {
                revealed = k
                delay(70)
            }
            delay(350)
            if (r.won) {
                stars = starsFor(moves, puzzle.par)
                saveStars(ctx, level.number, stars)
            }
            phase = Phase.DONE
        }
    }

    val r = result
    val position = remember(r) { r?.order?.withIndex()?.associate { it.value to it.index } ?: emptyMap() }
    fun isWet(i: Int) = (position[i] ?: Int.MAX_VALUE) < revealed

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Levels", color = TextDim) }
                Spacer(Modifier.weight(1f))
                Text("Level ${level.number}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (level.timeLimit != null) "⏱ ${timeLeft}s" else "",
                    color = if (level.timeLimit != null && timeLeft <= 10) Leak else TextDim,
                    fontSize = 16.sp, modifier = Modifier.width(72.dp), textAlign = TextAlign.End,
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("Moves $moves  ·  Par ${puzzle.par}", color = TextDim, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("Tank ${level.tankPressure}  ·  Need ${level.need}", color = TextDim, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Board
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val cell = minOf((maxWidth - 4.dp) / level.width, (maxHeight - 4.dp) / level.height)
                Column(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A131E)).padding(2.dp)
                ) {
                    for (row in 0 until level.height) {
                        Row {
                            for (col in 0 until level.width) {
                                val i = row * level.width + col
                                TileView(
                                    cell = level.cells[i],
                                    rot = rots[i],
                                    wet = isWet(i),
                                    leakMask = if (phase == Phase.DONE) r?.leakEnds?.get(i) ?: 0 else 0,
                                    delivered = if (isWet(i)) r?.fixturePressure?.get(i) else null,
                                    need = level.need,
                                    tank = level.tankPressure,
                                    sides = if (level.cells[i].kind == Kind.TANK || level.cells[i].kind == Kind.FIXTURE)
                                        connectedSides(level, rots, i) else 0,
                                    modifier = Modifier.size(cell),
                                    onTap = {
                                        if (phase == Phase.PLAYING && level.cells[i].kind == Kind.PIPE) {
                                            rots[i] = rots[i] + 1
                                            moves++
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(level.hint, color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    result = simulate(level, rots.toList())
                    revealed = 0
                    phase = Phase.FLOWING
                },
                enabled = phase == Phase.PLAYING,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Water),
            ) { Text("OPEN VALVE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }

        if (phase == Phase.DONE) {
            ResultCard(
                won = r?.won == true && !timedOut,
                stars = stars,
                message = if (timedOut) "Time's up!" else r?.reason ?: "",
                canFix = !timedOut,
                isLast = level.number == LEVELS.size,
                onNext = onNext,
                onRestart = { restart() },
                onFix = {
                    result = null; revealed = 0; phase = Phase.PLAYING
                },
                onLevels = onBack,
            )
        }
    }
}

@Composable
fun TileView(
    cell: Cell, rot: Int, wet: Boolean, leakMask: Int, delivered: Int?,
    need: Int, tank: Int, sides: Int, modifier: Modifier, onTap: () -> Unit,
) {
    val angle by animateFloatAsState(rot * 90f, tween(180, easing = FastOutSlowInEasing), label = "rot")
    val loop = rememberInfiniteTransition(label = "loop")
    val flow by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "flow",
    )
    val slow by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "slow",
    )
    val wobble by loop.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wobble",
    )
    val satisfied = delivered != null && delivered >= need
    Box(
        modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(5.dp))
            .clickable(enabled = cell.kind == Kind.PIPE) { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (cell.kind == Kind.WALL) drawBricks() else drawFloor()
            if (leakMask != 0) drawRect(Leak.copy(alpha = 0.22f))
        }
        when (cell.kind) {
            Kind.PIPE -> Canvas(
                Modifier.fillMaxSize().graphicsLayer { rotationZ = angle - rot * 90f }
            ) {
                drawPipe(rotateCW(cell.baseMask, rot), wet, flow)
                if (leakMask != 0) drawLeaks(leakMask, slow)
            }
            Kind.TANK -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawStubs(sides, true, flow)
                    drawTank(wobble)
                }
                Text(
                    "$tank", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(4.dp)).background(Color(0xCC0E1A2B)).padding(horizontal = 4.dp),
                )
            }
            Kind.FIXTURE -> {
                Canvas(Modifier.fillMaxSize()) {
                    if (satisfied) drawCircle(Ok.copy(alpha = 0.18f), size.minDimension * 0.48f)
                    drawStubs(sides, delivered != null, flow)
                    when (cell.fixture) {
                        FixtureType.SINK -> drawSink(satisfied, slow)
                        FixtureType.WC -> drawWC(satisfied, slow)
                        FixtureType.SHOWER -> drawShower(satisfied, flow)
                        FixtureType.KITCHEN -> drawKitchen(satisfied, slow)
                        null -> {}
                    }
                }
                val pill = when {
                    delivered == null -> Color(0xCC0E1A2B)
                    satisfied -> Ok
                    else -> Leak
                }
                Text(
                    if (delivered != null) "$delivered" else "≥$need",
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                        .clip(RoundedCornerShape(4.dp)).background(pill).padding(horizontal = 3.dp),
                )
            }
            else -> {}
        }
    }
}

@Composable
fun ResultCard(
    won: Boolean, stars: Int, message: String, canFix: Boolean, isLast: Boolean,
    onNext: () -> Unit, onRestart: () -> Unit, onFix: () -> Unit, onLevels: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FixtureBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (won) {
                Text("Flowing!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(starText(stars), color = Gold, fontSize = 40.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = if (isLast) onLevels else onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Water),
                ) { Text(if (isLast) "World complete!" else "Next level", color = Color.White) }
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("Replay for more stars", color = Color.White)
                }
            } else {
                Text("Not yet", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(message, color = TextDim, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                if (canFix) {
                    Button(
                        onClick = onFix,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Water),
                    ) { Text("Fix it", color = Color.White) }
                }
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("Restart level", color = Color.White)
                }
            }
            TextButton(onClick = onLevels) { Text("Levels", color = TextDim) }
        }
    }
}
