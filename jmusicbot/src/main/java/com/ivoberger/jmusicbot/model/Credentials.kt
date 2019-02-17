package com.ivoberger.jmusicbot.model

import android.util.Base64
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

sealed class Credentials {
    @JsonClass(generateAdapter = true)
    class Register(
        @Json(name = "name") val name: String,
        @Json(name = "userId") val uuid: String = UUID.randomUUID().toString()
    ) : Credentials() {
        constructor(user: User) : this(user.name, user.id)
    }

    class Login(
        val name: String, val password: String
    ) : Credentials() {
        constructor(user: User) : this(
            user.name,
            user.password ?: throw InvalidParametersException(
                InvalidParametersException.Type.INVALID_PASSWORD,
                "Password can't be null"
            )
        )

        override fun toString(): String =
            "Basic ${Base64.encodeToString("$name:$password".toByteArray(), Base64.DEFAULT).trim()}"
    }

    class PasswordChange(@Json(name = "newPassword") val newPassword: String)
}
