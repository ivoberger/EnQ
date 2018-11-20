package me.iberger.enq

import me.iberger.jmusicbot.KEY_QUEUE

const val KEY_DEFAULT = "default"
const val KEY_FAVOURITES = "favorites"
const val KEY_SUGGESTIONS = "favorites"

enum class TABS(val key: String) {
    QUEUE(KEY_QUEUE), SUGGESTIONS(KEY_SUGGESTIONS), FAVOURITES(KEY_FAVOURITES)
}