# Full Pressure

A pipe puzzle game for Android. Turn the pipes to get water from the tank to every fixture, with enough pressure left to make each one flow.

- 10 levels (World 1 · Villa), stars by moves vs. par, timers from level 5
- Each pipe costs pressure: straight 1, elbow/tee 2, cross 3
- Levels live in `Levels.kt`, drawn in their solved state; the game scrambles them

Every push to `main` builds `FullPressure.apk` and attaches it to the **latest** release.
