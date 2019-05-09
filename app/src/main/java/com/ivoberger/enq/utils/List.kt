package com.ivoberger.enq.utils

fun <T> List<T>.addToEnd(new: T): List<T> =
    if (!contains(new)) this + new
    else this.toMutableList().apply {
        remove(new)
        add(new)
    }
