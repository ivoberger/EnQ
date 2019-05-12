/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.jmusicbot.api

import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.model.Auth
import com.ivoberger.jmusicbot.model.AuthExpectation
import com.ivoberger.jmusicbot.model.AuthType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? = runBlocking(Dispatchers.IO) {
        Timber.d("Re-authorizing")
        var auth: String? = null
        response.body()?.let { body ->
            val authExpectation =
                JMusicBot.mBaseComponent.moshi.adapter(AuthExpectation::class.java).fromJson(String(body.bytes()))
            Timber.d("AuthExpectation: $authExpectation")
            auth = when (authExpectation?.format) {
                AuthType.BASIC -> {
                    Timber.d("BASIC Auth")
                    JMusicBot.user?.let { Auth.Basic(it).toAuthHeader() }
                }
                AuthType.TOKEN -> {
                    Timber.d("TOKEN Auth")
                    if (!JMusicBot.state.hasServer) {
                        JMusicBot.discoverHost()
                        JMusicBot.state.running?.join()
                    }
                    JMusicBot.authorize()
                    JMusicBot.authToken?.toAuthHeader()
                }
                else -> null
            }
        }
        val origRequest = response.request()

        return@runBlocking if (origRequest.header(KEY_AUTHORIZATION) == auth) null else origRequest.newBuilder()
            .header(KEY_AUTHORIZATION, auth ?: "")
            .build()
    }
}
