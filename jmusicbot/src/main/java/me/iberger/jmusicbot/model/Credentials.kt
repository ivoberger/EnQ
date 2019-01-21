package me.iberger.jmusicbot.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import java.util.*

sealed class Credentials {
    @JsonClass(generateAdapter = true)
    class Register(
        @Json(name = "name") val name: String,
        @Json(name = "uuid") val uuid: String = UUID.randomUUID().toString()
    ) : Credentials() {
        constructor(user: User) : this(user.name, user.uuid)
    }

    @JsonClass(generateAdapter = true)
    class Login(
        @Json(name = "name") val name: String,
        @Json(name = "password") val password: String,
        @Json(name = "uuid") val uuid: String = UUID.randomUUID().toString()
    ) : Credentials() {
        constructor(user: User) : this(
            user.name,
            user.password ?: throw InvalidParametersException(
                InvalidParametersException.Type.INVALID_PASSWORD,
                "Password can't be null"
            ),
            user.uuid
        )
    }

    class PasswordChange(@Json(name = "newPassword") val newPassword: String)
}
