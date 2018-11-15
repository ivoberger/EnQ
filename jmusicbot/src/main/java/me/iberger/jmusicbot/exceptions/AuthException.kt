package me.iberger.jmusicbot.exceptions

class AuthException : Exception {
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


class ServerErrorException(code: Int) : Exception("Server error $code")