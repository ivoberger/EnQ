package com.ivoberger.jmusicbot.api

import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.model.toHTTPAuth
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        Timber.d("Retrieving new token NP")
        JMusicBot.authorize()
        return@runBlocking response.request().newBuilder()
            .header(KEY_AUTHORIZATION, JMusicBot.authToken?.toHTTPAuth() ?: "")
            .build()
    }
}
