package com.midotheeng.fullpressure

// Each level is drawn in its SOLVED state; the game scrambles the pipes.
// T = tank, S = sink, W = WC, H = shower, K = kitchen, # = wall, . = empty.
// Pipes not on the route are decoys.
val LEVELS: List<Level> = listOf(
    Level(1, listOf(
        "....",
        "T──S",
        "....",
        "....",
    ), 20, 1, null, "Tap a pipe to turn it. Connect the tank to the sink, then open the valve."),
    Level(2, listOf(
        "T─┐.",
        "..│.",
        "..└S",
        "....",
    ), 20, 1, null, "Elbows turn corners."),
    Level(3, listOf(
        ".....",
        "T─┐..",
        "..│..",
        "..└─W",
        ".....",
    ), 20, 1, null, "Not sure? Open the valve to test your route."),
    Level(4, listOf(
        ".....",
        "T─┬─S",
        "..│..",
        "..W..",
        ".....",
    ), 20, 1, null, "A tee splits the flow between two fixtures."),
    Level(5, listOf(
        "S....",
        "│.─..",
        "├─┐.┐",
        "T.│..",
        "..└─W",
    ), 20, 1, 90, "Watch for leaks: any open pipe end spills water."),
    Level(6, listOf(
        "T─┐#S",
        "##│#│",
        "┌─┘#│",
        "├W##│",
        "└───┘",
    ), 30, 2, 90, "Walls block the obvious route. Go around."),
    Level(7, listOf(
        ".─....",
        "T──┬─S",
        "┐..│.└",
        ".┌─┴─┐",
        ".│.┼.│",
        ".W...K",
    ), 20, 3, 90, "Not every pipe is needed."),
    Level(8, listOf(
        "┌.H...",
        "..│...",
        "T─┼──S",
        "..│.─.",
        "..└─┐.",
        "┘...W.",
    ), 20, 3, 75, "A cross sends water four ways."),
    Level(9, listOf(
        "S─┐..┐",
        "┬.│.H.",
        "..├─┘.",
        "T─┤...",
        "..│.│.",
        "..└──K",
    ), 20, 4, 75, "Match the par moves to earn 3 stars."),
    Level(10, listOf(
        "H─┐.W.",
        "└.│.│.",
        "T─┼─┴S",
        "..│.┬.",
        "┌─┘...",
        "S..─..",
    ), 20, 5, 60, "The full bathroom. Every fixture needs pressure 5."),
)
