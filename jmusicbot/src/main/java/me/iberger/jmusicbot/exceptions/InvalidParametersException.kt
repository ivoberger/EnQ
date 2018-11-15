package me.iberger.jmusicbot.exceptions

class InvalidParametersException : Exception {
    val type: Type

    constructor(type: Type) : super() {
        this.type = type
    }

    constructor(type: Type, message: String) : super(message) {
        this.type = type
    }

    enum class Type {
        MISSING, INVALID_PASSWORD
    }
}
