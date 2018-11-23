package me.iberger.jmusicbot.network

import me.iberger.jmusicbot.KEY_AUTHORIZATION
import me.iberger.jmusicbot.MusicBot
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.d("Retrieving new token NP")
        val newToken = MusicBot.instance.refreshToken().body()!!
        MusicBot.instance.authToken = newToken
        return response.request().newBuilder().header(KEY_AUTHORIZATION, newToken).build()
    }
}