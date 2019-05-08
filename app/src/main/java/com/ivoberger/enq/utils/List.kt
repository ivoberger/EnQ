package com.ivoberger.enq.utils

fun <T> List<T>.addIfNotExistent(new: T): List<T> =
    if (!contains(new)) this + new
    else this
