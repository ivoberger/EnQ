package me.iberger.jmusicbot.exceptions

class AuthException : Throwable {
    val reason: Reason

    constructor(reason: Reason) {
        this.reason = reason
    }

    constructor(reason: Reason, message: String) : super(message) {
        this.reason = reason
    }

    enum class Reason() {
        NEEDS_AUTH, NEEDS_PERMISSION
    }
}

class UsernameTakenException : Throwable("Username already in use")


class ServerErrorException(code: Int) : Throwable("Server error $code")