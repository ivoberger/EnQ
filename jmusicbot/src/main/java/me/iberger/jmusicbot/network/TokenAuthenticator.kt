package me.iberger.jmusicbot.network

import kotlinx.coroutines.runBlocking
import me.iberger.jmusicbot.KEY_AUTHORIZATION
import me.iberger.jmusicbot.NewMusicBot
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        Timber.d("Retrieving new token NP")
        NewMusicBot.refreshToken()
        return@runBlocking response.request().newBuilder().header(KEY_AUTHORIZATION, NewMusicBot.authToken ?: "")
            .build()
    }
}
