package com.midotheeng.fullpressure

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange

/**
 * Cloud save with Google Play Games. Progress (stars per level) is stored in one saved game
 * on the player's Google account, so it comes back on any phone.
 * Stays switched off until a Play Games project ID is set in res/values/games.xml.
 */
object CloudSave {
    private const val SAVE_NAME = "full-pressure-progress"

    enum class Status { OFF, SIGNED_OUT, CONNECTED }

    fun configured(ctx: Context) = ctx.getString(R.string.game_services_project_id).isNotBlank()

    fun init(ctx: Context) {
        if (configured(ctx)) PlayGamesSdk.initialize(ctx.applicationContext)
    }

    /** Checks the (usually automatic) Play Games sign-in. */
    fun check(activity: Activity, done: (Status) -> Unit) {
        if (!configured(activity)) return done(Status.OFF)
        PlayGames.getGamesSignInClient(activity).isAuthenticated.addOnCompleteListener { task ->
            done(if (task.isSuccessful && task.result.isAuthenticated) Status.CONNECTED else Status.SIGNED_OUT)
        }
    }

    /** Asks the player to sign in to Play Games. */
    fun signIn(activity: Activity, done: (Status) -> Unit) {
        if (!configured(activity)) return done(Status.OFF)
        PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener { task ->
            done(if (task.isSuccessful && task.result.isAuthenticated) Status.CONNECTED else Status.SIGNED_OUT)
        }
    }

    private fun encode(stars: Map<Int, Int>) = stars.entries.joinToString(",") { "${it.key}:${it.value}" }.toByteArray()

    private fun decode(bytes: ByteArray): Map<Int, Int> =
        String(bytes).split(",").mapNotNull {
            val p = it.split(":")
            if (p.size == 2) p[0].toIntOrNull()?.let { k -> p[1].toIntOrNull()?.let { v -> k to v } } else null
        }.toMap()

    /**
     * Downloads the cloud progress, merges it with this phone's (best stars per level win),
     * saves the merged result in both places and reports the merged stars.
     */
    fun sync(activity: Activity, local: Map<Int, Int>, done: (Map<Int, Int>) -> Unit) {
        if (!configured(activity)) return done(local)
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(SAVE_NAME, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnSuccessListener { result ->
                val snapshot = result.data ?: return@addOnSuccessListener done(local)
                val cloud = try { decode(snapshot.snapshotContents.readFully()) } catch (e: Exception) { emptyMap() }
                val merged = HashMap(local)
                for ((k, v) in cloud) if (v > (merged[k] ?: 0)) merged[k] = v
                snapshot.snapshotContents.writeBytes(encode(merged))
                val total = merged.values.sum()
                client.commitAndClose(
                    snapshot,
                    SnapshotMetadataChange.Builder().setDescription("Full Pressure · $total stars").build(),
                ).addOnCompleteListener { done(merged) }
            }
            .addOnFailureListener { done(local) }
    }
}
