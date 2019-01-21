package me.iberger.jmusicbot.exceptions

class QueueException : Exception {
    val reason: Reason

    constructor(reason: Reason) : super() {
        this.reason = reason
    }

    constructor(reason: Reason, message: String) : super(message) {
        this.reason = reason
    }

    enum class Reason {
        PARAM_MISSING, NOT_FOUND
    }
}
