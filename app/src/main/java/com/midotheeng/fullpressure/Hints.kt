package com.midotheeng.fullpressure

import android.content.Context

/**
 * Solution hints: up to 3 stored. Each one used comes back after 10 minutes.
 */
object Hints {
    const val MAX = 3
    const val REFILL_MS = 10 * 60 * 1000L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("hints", Context.MODE_PRIVATE)

    /** Current (count, milliseconds until the next one comes back; 0 when full). */
    fun state(ctx: Context, now: Long = System.currentTimeMillis()): Pair<Int, Long> {
        val p = prefs(ctx)
        var count = p.getInt("count", MAX)
        var since = p.getLong("since", now)
        if (count < MAX) {
            val gained = ((now - since) / REFILL_MS).toInt()
            if (gained > 0) {
                count = minOf(MAX, count + gained)
                since = if (count == MAX) now else since + gained * REFILL_MS
                p.edit().putInt("count", count).putLong("since", since).apply()
            }
        }
        val wait = if (count >= MAX) 0L else (since + REFILL_MS - now).coerceAtLeast(0L)
        return count to wait
    }

    /** Use one hint. Returns false if none are left. */
    fun use(ctx: Context): Boolean {
        val now = System.currentTimeMillis()
        val (count, _) = state(ctx, now)
        if (count <= 0) return false
        val p = prefs(ctx)
        val e = p.edit().putInt("count", count - 1)
        if (count == MAX) e.putLong("since", now)   // the refill clock starts with the first one used
        e.apply()
        return true
    }
}

fun formatWait(ms: Long): String {
    val total = (ms + 999) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
