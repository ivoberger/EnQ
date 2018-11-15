package me.iberger.jmusicbot.data

import java.util.*

sealed class Credentials {
    class Register(
        val name: String,
        val userId: String = UUID.randomUUID().toString()
    ) : Credentials()

    class Token(val token: String) : Credentials()
}