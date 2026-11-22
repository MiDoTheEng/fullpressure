package com.midotheeng.fullpressure

import java.util.PriorityQueue
import java.util.Random

// Directions as bit flags. A pipe's openings are a combination of these.
const val N = 1
const val E = 2
const val S = 4
const val W = 8

data class Dir(val bit: Int, val dr: Int, val dc: Int)

val DIRS = listOf(Dir(N, -1, 0), Dir(E, 0, 1), Dir(S, 1, 0), Dir(W, 0, -1))

fun opposite(bit: Int): Int = when (bit) {
    N -> S
    S -> N
    E -> W
    else -> E
}

/** Rotate an opening mask clockwise by [times] quarter turns. */
fun rotateCW(mask: Int, times: Int): Int {
    var m = mask
    repeat(((times % 4) + 4) % 4) { m = ((m shl 1) or (m shr 3)) and 15 }
    return m
}

enum class Kind { EMPTY, WALL, TANK, FIXTURE, PIPE }

enum class Room(val label: String, val world: String) {
    KITCHEN("Kitchen", "World 1 · Kitchen"),
    BATHROOM("Bathroom", "World 2 · Bathrooms"),
    GARDEN("Garden", "World 3 · Garden"),
    VILLA("Villa", "World 4 · Villa"),
    TOWER("Sky Tower", "World 5 · Sky Tower"),
}

enum class FixtureType(val label: String, val ch: Char) {
    SINK("Basin", 'S'),
    SHOWER("Shower", 'H'),
    BATH("Bathtub", 'B'),
    KITCHEN("Sink", 'K'),
    DISHWASHER("Dishwasher", 'D'),
    WASHER("Washer", 'M'),
    GARDEN_TAP("Tap", 'G'),
    SPRINKLER("Sprinkler", 'P'),
    FOUNTAIN("Fountain", 'N'),
}

data class Cell(
    val kind: Kind,
    val baseMask: Int = 0,     // openings in the solved orientation
    val cost: Int = 0,         // pressure lost passing through
    val fixture: FixtureType? = null,
)

// Pipe pieces written in their solved orientation: char -> (openings, pressure cost)
private val PIPES: Map<Char, Pair<Int, Int>> = mapOf(
    '│' to Pair(N or S, 1),
    '─' to Pair(E or W, 1),
    '└' to Pair(N or E, 2),
    '┌' to Pair(E or S, 2),
    '┐' to Pair(S or W, 2),
    '┘' to Pair(W or N, 2),
    '├' to Pair(N or E or S, 2),
    '┬' to Pair(E or S or W, 2),
    '┤' to Pair(S or W or N, 2),
    '┴' to Pair(W or N or E, 2),
    '┼' to Pair(15, 3),
)

private fun parseCell(ch: Char): Cell {
    PIPES[ch]?.let { return Cell(Kind.PIPE, it.first, it.second) }
    FixtureType.entries.firstOrNull { it.ch == ch }?.let { return Cell(Kind.FIXTURE, fixture = it) }
    return when (ch) {
        'T' -> Cell(Kind.TANK)
        '#' -> Cell(Kind.WALL)
        else -> Cell(Kind.EMPTY)
    }
}

class Level(
    val number: Int,
    val room: Room,
    rows: List<String>,
    val tankPressure: Int,
    val need: Int,              // minimum pressure every fixture needs
    val waterIn: Int,           // seconds before the water starts flowing
    val hint: String,
) {
    val height = rows.size
    val width = rows[0].length
    val cells: List<Cell> = rows.flatMap { row -> row.map { parseCell(it) } }
    val fixtureCount = cells.count { it.kind == Kind.FIXTURE }
}

class FlowResult(
    val order: List<Int>,                 // cells in the order water reaches them (pipes and fixtures)
    val depth: Map<Int, Int>,             // how many steps from the tank each wet cell is
    val inlet: Map<Int, Int>,             // the side water enters each wet cell from
    val pressure: Map<Int, Int>,          // pressure left in each filled pipe
    val leaks: Set<Int>,                  // filled pipes with an open end
    val leakEnds: Map<Int, Int>,          // which sides of each leaking pipe are open
    val fixturePressure: Map<Int, Int>,   // pressure delivered to each fixture
    val won: Boolean,
    val reason: String,
)

/** Run water from the tank through the board, using each pipe's current rotation. */
fun simulate(level: Level, rots: List<Int>): FlowResult {
    val w = level.width
    val h = level.height
    val cells = level.cells
    fun maskAt(i: Int) = if (cells[i].kind == Kind.PIPE) rotateCW(cells[i].baseMask, rots[i]) else 0
    fun inside(r: Int, c: Int) = r in 0 until h && c in 0 until w

    val best = HashMap<Int, Int>()
    val order = ArrayList<Int>()
    val fixturePressure = HashMap<Int, Int>()
    val depth = HashMap<Int, Int>()
    val inlet = HashMap<Int, Int>()
    val queue = PriorityQueue<IntArray>(compareByDescending<IntArray> { it[0] })

    // The tank feeds any neighbouring pipe that faces it.
    for (i in cells.indices) {
        if (cells[i].kind != Kind.TANK) continue
        val r = i / w
        val c = i % w
        for (d in DIRS) {
            val rr = r + d.dr
            val cc = c + d.dc
            if (!inside(rr, cc)) continue
            val j = rr * w + cc
            if (cells[j].kind == Kind.PIPE && maskAt(j) and opposite(d.bit) != 0) {
                val p = level.tankPressure - cells[j].cost
                if (p > 0) queue.add(intArrayOf(p, j, opposite(d.bit), 0))
            }
        }
    }

    // Highest pressure wins, like water taking the easiest route.
    while (queue.isNotEmpty()) {
        val entry = queue.poll()!!
        val p = entry[0]
        val i = entry[1]
        if (best.containsKey(i)) continue
        best[i] = p
        inlet[i] = entry[2]
        depth[i] = entry[3]
        order.add(i)
        val r = i / w
        val c = i % w
        val m = maskAt(i)
        for (d in DIRS) {
            if (m and d.bit == 0) continue
            val rr = r + d.dr
            val cc = c + d.dc
            if (!inside(rr, cc)) continue
            val j = rr * w + cc
            when (cells[j].kind) {
                Kind.FIXTURE -> {
                    if (!fixturePressure.containsKey(j)) {
                        order.add(j)
                        inlet[j] = opposite(d.bit)
                        depth[j] = depth[i]!! + 1
                    }
                    fixturePressure[j] = maxOf(fixturePressure[j] ?: 0, p)
                }
                Kind.PIPE -> if (!best.containsKey(j) && maskAt(j) and opposite(d.bit) != 0) {
                    val q = p - cells[j].cost
                    if (q > 0) queue.add(intArrayOf(q, j, opposite(d.bit), depth[i]!! + 1))
                }
                else -> {}
            }
        }
    }

    // Any filled pipe pointing at nothing is a leak.
    val leaks = HashSet<Int>()
    val leakEnds = HashMap<Int, Int>()
    fun leak(i: Int, bit: Int) { leaks.add(i); leakEnds[i] = (leakEnds[i] ?: 0) or bit }
    for (i in best.keys) {
        val r = i / w
        val c = i % w
        val m = maskAt(i)
        for (d in DIRS) {
            if (m and d.bit == 0) continue
            val rr = r + d.dr
            val cc = c + d.dc
            if (!inside(rr, cc)) { leak(i, d.bit); continue }
            val j = rr * w + cc
            val ok = when (cells[j].kind) {
                Kind.TANK, Kind.FIXTURE -> true
                Kind.PIPE -> maskAt(j) and opposite(d.bit) != 0
                else -> false
            }
            if (!ok) leak(i, d.bit)
        }
    }

    val reached = fixturePressure.size
    val low = fixturePressure.values.any { it < level.need }
    val reason = when {
        leaks.isNotEmpty() -> "Leak! Water is spilling out of an open pipe."
        reached < level.fixtureCount -> "A fixture got no water."
        low -> "Not enough pressure. Try a shorter route with fewer bends."
        else -> ""
    }
    return FlowResult(order, depth, inlet, best, leaks, leakEnds, fixturePressure, reason.isEmpty(), reason)
}

class Puzzle(val start: List<Int>, val par: Int)

/** Scramble the solved layout so the level starts unsolved. Same layout every time for a level. */
fun makePuzzle(level: Level): Puzzle {
    val cells = level.cells
    val solved = List(cells.size) { 0 }
    val path = simulate(level, solved).pressure.keys
    val rnd = Random(level.number * 7919L)
    var rots = solved
    for (attempt in 0 until 50) {
        val r = MutableList(cells.size) { i -> if (cells[i].kind == Kind.PIPE) rnd.nextInt(4) else 0 }
        for (i in path) {
            val base = cells[i].baseMask
            if (rotateCW(base, r[i]) == base && base != 15) r[i] = (r[i] + 1) % 4
        }
        rots = r
        if (!simulate(level, r).won) break
    }
    // Par = fewest taps (each tap turns clockwise) to fix every pipe on the route.
    var par = 0
    for (i in path) {
        val base = cells[i].baseMask
        par += (0..3).first { k -> rotateCW(base, rots[i] + k) == base }
    }
    return Puzzle(rots, par)
}

fun starsFor(moves: Int, par: Int): Int = when {
    moves <= par -> 3
    moves <= par + maxOf(2, par / 2) -> 2
    else -> 1
}

/** Sides of cell [i] where a neighbouring pipe currently points into it. */
fun connectedSides(level: Level, rots: List<Int>, i: Int): Int {
    val r = i / level.width
    val c = i % level.width
    var sides = 0
    for (d in DIRS) {
        val rr = r + d.dr
        val cc = c + d.dc
        if (rr !in 0 until level.height || cc !in 0 until level.width) continue
        val j = rr * level.width + cc
        val cell = level.cells[j]
        if (cell.kind == Kind.PIPE && rotateCW(cell.baseMask, rots[j]) and opposite(d.bit) != 0) sides = sides or d.bit
    }
    return sides
}
