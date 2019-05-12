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
package com.ivoberger.jmusicbot.di

import com.ivoberger.jmusicbot.api.MusicBotService
import com.ivoberger.jmusicbot.api.TokenAuthenticator
import com.ivoberger.jmusicbot.api.withToken
import com.ivoberger.jmusicbot.model.Auth
import com.ivoberger.jmusicbot.model.User
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named

@Module
internal class UserModule(private val mUser: User, private val mAuthToken: Auth.Token) {
    @Provides
    fun user(): User = mUser

    @Provides
    fun authToken(): Auth.Token = mAuthToken

    @Provides
    @Named(NameKeys.RETROFIT_AUTHENTICATED)
    fun retrofit(
        @Named(NameKeys.BUILDER_RETROFIT_URL) retrofitBuilder: Retrofit.Builder,
        client: OkHttpClient
    ): Retrofit = retrofitBuilder.client(client).build()

    @Provides
    fun okHttpClient(okHttpClientBuilder: OkHttpClient.Builder, authToken: Auth.Token) =
        okHttpClientBuilder.authenticator(TokenAuthenticator()).withToken(authToken)

    @Provides
    @Named(NameKeys.SERVICE_AUTHENTICATED)
    fun musicBotService(@Named(NameKeys.RETROFIT_AUTHENTICATED) retrofit: Retrofit): MusicBotService =
        retrofit.create(MusicBotService::class.java)
}