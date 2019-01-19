package me.iberger.jmusicbot.network

import kotlinx.coroutines.runBlocking
import me.iberger.jmusicbot.KEY_AUTHORIZATION
import me.iberger.jmusicbot.MusicBot
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        Timber.d("Retrieving new token NP")
        val newToken = MusicBot.instance?.refreshToken()?.body()!!
        MusicBot.instance?.authToken = newToken
        return@runBlocking response.request().newBuilder().header(KEY_AUTHORIZATION, newToken).build()
    }
}
