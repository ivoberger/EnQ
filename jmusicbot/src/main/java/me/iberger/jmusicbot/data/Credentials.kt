package me.iberger.jmusicbot.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.util.*

sealed class Credentials {
    @JsonClass(generateAdapter = true)
    class Register(
        val name: String,
        val uuid: String = UUID.randomUUID().toString()
    ) : Credentials()

    @JsonClass(generateAdapter = true)
    class Token(val token: String) : Credentials() {
        @ToJson
        fun toJson(token: Credentials.Token) = token.token

        @FromJson
        fun fromJson(token: String): Credentials.Token {
            Timber.d("Creating Token: $token")
            return Credentials.Token(token)
        }
    }

    class PasswordChange(val newPassword: String)
}