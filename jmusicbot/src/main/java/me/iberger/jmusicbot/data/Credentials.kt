package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import java.util.*

sealed class Credentials {
    @JsonClass(generateAdapter = true)
    class Register(
        val name: String,
        val uuid: String = UUID.randomUUID().toString()
    ) : Credentials() {
        constructor(user: User) : this(user.name, user.uuid)
    }

    @JsonClass(generateAdapter = true)
    class Login(
        val name: String,
        val uuid: String = UUID.randomUUID().toString(),
        val password: String
    ) : Credentials() {
        constructor(user: User) : this(
            user.name,
            user.uuid,
            user.password ?: throw InvalidParametersException(
                InvalidParametersException.Type.INVALID_PASSWORD,
                "Password can't be null"
            )
        )
    }

    class PasswordChange(val newPassword: String)
}