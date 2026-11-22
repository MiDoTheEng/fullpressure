package com.midotheeng.fullpressure

// Each level is drawn in its SOLVED state; the game scrambles the pipes.
// T = tank, # = wall/counter, . = floor. Bathroom: S basin, H shower, B bathtub.
// Kitchen: K sink, D dishwasher, M washing machine. Pipes not on the route are decoys.
val LEVELS: List<Level> = listOf(
    Level(1, Room.BATHROOM, listOf(
        "....",
        "T──S",
        "....",
        "....",
    ), 20, 1, null, "Tap a pipe to turn it. Connect the tank to the basin, then open the valve."),
    Level(2, Room.BATHROOM, listOf(
        "T─┐.",
        "..│.",
        "..└S",
        "....",
    ), 20, 1, null, "Elbows turn corners."),
    Level(3, Room.BATHROOM, listOf(
        ".....",
        "T─┐..",
        "..│..",
        "..└─H",
        ".....",
    ), 20, 1, null, "Not sure? Open the valve to test your route."),
    Level(4, Room.KITCHEN, listOf(
        ".....",
        "T─┬─K",
        "..│..",
        "..D..",
        ".....",
    ), 20, 1, null, "A tee splits the flow between the sink and the dishwasher."),
    Level(5, Room.BATHROOM, listOf(
        "S....",
        "│.─..",
        "├─┐.┐",
        "T.│..",
        "..└─H",
    ), 20, 1, 90, "Watch for leaks: any open pipe end spills water."),
    Level(6, Room.KITCHEN, listOf(
        "T─┐#K",
        "##│#│",
        "┌─┘#│",
        "├M##│",
        "└───┘",
    ), 30, 2, 90, "The counters are in the way. Go around them."),
    Level(7, Room.BATHROOM, listOf(
        ".─....",
        "T──┬─S",
        "┐..│.└",
        ".┌─┴─┐",
        ".│.┼.│",
        ".B...H",
    ), 20, 3, 90, "Not every pipe is needed."),
    Level(8, Room.KITCHEN, listOf(
        "┌.K...",
        "..│...",
        "T─┼──D",
        "..│.─.",
        "..└─┐.",
        "┘...M.",
    ), 20, 3, 75, "A cross sends water four ways."),
    Level(9, Room.BATHROOM, listOf(
        "S─┐..┐",
        "┬.│.H.",
        "..├─┘.",
        "T─┤...",
        "..│.│.",
        "..└──B",
    ), 20, 4, 75, "Match the par moves to earn 3 stars."),
    Level(10, Room.BATHROOM, listOf(
        "H─┐.S.",
        "└.│.│.",
        "T─┼─┴B",
        "..│.┬.",
        "┌─┘...",
        "S..─..",
    ), 20, 5, 60, "The full bathroom. Every fixture needs pressure 5."),
)
