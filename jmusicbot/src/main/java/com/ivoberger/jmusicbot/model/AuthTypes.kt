package com.ivoberger.jmusicbot.model

import android.util.Base64
import com.auth0.android.jwt.JWT
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

sealed class AuthTypes {

    @JsonClass(generateAdapter = true)
    class Register(
        @Json(name = "name") val name: String,
        @Json(name = "userId") val uuid: String = UUID.randomUUID().toString()
    ) : AuthTypes() {
        constructor(user: User) : this(user.name, user.id)
    }

    class Basic(
        val name: String, val password: String
    ) : AuthTypes() {
        constructor(user: User) : this(
            user.name,
            user.password ?: throw InvalidParametersException(
                InvalidParametersException.Type.INVALID_PASSWORD,
                "Password can't be null"
            )
        )

        fun toAuthHeader(): String =
            "Basic ${Base64.encodeToString("$name:$password".toByteArray(), Base64.DEFAULT).trim()}"
    }

    class Token(private val token: String) {

        private val jwt by lazy { JWT(token) }
        val permissions by lazy { Permissions.fromClaims(jwt.claims) }

        override fun toString(): String = token
        fun toAuthHeader() = "Bearer $token"
        fun isExpired() = jwt.isExpired(60)
    }

    @JsonClass(generateAdapter = true)
    class PasswordChange(@Json(name = "newPassword") val newPassword: String)
}

@JsonClass(generateAdapter = true)
data class AuthExpectation(val format: AuthType, val type: String?, val permissions: List<Permissions>?)

enum class AuthType {
    @Json(name = "Basic")
    BASIC,
    @Json(name = "Token")
    TOKEN
}
