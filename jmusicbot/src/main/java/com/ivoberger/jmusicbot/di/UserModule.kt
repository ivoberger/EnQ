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
        @Named(NameKeys.BUILDER_RETROFIT_URL) retrofitBuilder: Retrofit.Builder, client: OkHttpClient
    ): Retrofit = retrofitBuilder.client(client).build()

    @Provides
    fun okHttpClient(okHttpClientBuilder: OkHttpClient.Builder, authToken: Auth.Token) =
        okHttpClientBuilder.authenticator(TokenAuthenticator()).withToken(authToken)

    @Provides
    @Named(NameKeys.SERVICE_AUTHENTICATED)
    fun musicBotService(@Named(NameKeys.RETROFIT_AUTHENTICATED) retrofit: Retrofit): MusicBotService =
        retrofit.create(MusicBotService::class.java)
}