package me.iberger.jmusicbot.model

import com.auth0.android.jwt.Claim

enum class Permissions {
    MOVE, SKIP, DISLIKE;

    companion object {
        fun fromClaims(claims: Map<String, Claim>): MutableList<Permissions> {
            val permissions = mutableListOf<Permissions>()
            claims.forEach {
                if (it.value.asBoolean() == true) when (it.key.toUpperCase()) {
                    MOVE.toString() -> permissions.add(MOVE)
                    SKIP.toString() -> permissions.add(SKIP)
                    DISLIKE.toString() -> permissions.add(DISLIKE)
                }
            }
            return permissions
        }
    }
}
