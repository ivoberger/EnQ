package com.ivoberger.jmusicbot.api

import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.model.AuthExpectation
import com.ivoberger.jmusicbot.model.AuthType
import com.ivoberger.jmusicbot.model.AuthTypes
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        Timber.d("Re-authorizing")
        var auth: String? = null
        response.body()?.let { body ->
            val authExpectation = JMusicBot.mMoshi.adapter(AuthExpectation::class.java).fromJson(String(body.bytes()))
            Timber.d("AuthExpectation: $authExpectation")
            auth = when (authExpectation?.format) {
                AuthType.BASIC -> JMusicBot.user?.let { AuthTypes.Basic(it).toAuthHeader() }
                AuthType.TOKEN -> {
                    JMusicBot.authorize()
                    JMusicBot.authToken?.toAuthHeader()
                }
                else -> null
            }
        }

        return@runBlocking response.request().newBuilder()
            .header(KEY_AUTHORIZATION, auth ?: "")
            .build()
    }
}
