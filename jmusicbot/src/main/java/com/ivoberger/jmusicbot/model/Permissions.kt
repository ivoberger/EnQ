package com.ivoberger.jmusicbot.model

import com.auth0.android.jwt.Claim
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import timber.log.Timber


enum class Permissions(val label: String) {
    /**
     * The permission to skip the current song or remove songs from the queue.
     * Note that a user is always allowed to remove a song from the queue which he added himself.
     */
    @Json(name = "skip")
    SKIP("skip"),

    /**
     * The permission to remove songs from the upcoming suggestions of a suggester.
     */
    @Json(name = "dislike")
    DISLIKE("dislike"),

    /**
     * The permission to move songs around in the queue.
     */
    @Json(name = "move")
    MOVE("move"),

    /**
     * Pause/resume current song.
     */
    @Json(name = "pause")
    PAUSE("pause"),

    /**
     * Put new songs into the queue.
     */
    @Json(name = "enqueue")
    ENQUEUE("enqueue"),

    /**
     * Songs enqueued by users without this permission do not affect suggestions.
     */
    @Json(name = "alter_suggestions")
    ALTER_SUGGESTIONS("alter_suggestions");

    companion object {
        fun fromClaims(claims: Map<String, Claim>): MutableList<Permissions> {
            val permissions = mutableListOf<Permissions>()
            claims["permissions"]?.asList(String::class.java)?.forEach {
                try {
                    permissions.add(valueOf(it.toUpperCase()))
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Unknown Permission: ${it.toUpperCase()}")
                }
            }
            Timber.d("Retrieved permissions from jwt claims: $permissions")
            return permissions
        }

        @FromJson
        fun fromJson(permString: String) = Permissions.valueOf(permString.toUpperCase())
    }
}
