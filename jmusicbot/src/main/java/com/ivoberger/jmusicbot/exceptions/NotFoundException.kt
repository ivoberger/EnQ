package com.ivoberger.jmusicbot.exceptions

class NotFoundException : Exception {
    val type: Type

    constructor(type: Type) : super() {
        this.type = type
    }

    constructor(type: Type, message: String) : super(message) {
        this.type = type
    }

    enum class Type {
        SONG, USER, PROVIDER, SUGGESTER
    }
}
