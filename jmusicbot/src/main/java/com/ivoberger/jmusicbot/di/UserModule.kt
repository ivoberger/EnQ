package com.ivoberger.jmusicbot.di

import com.ivoberger.jmusicbot.api.MusicBotService
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
        @Named(NameKeys.OKHTTP_AUTHENTICATED) client: OkHttpClient
    ): Retrofit = retrofitBuilder.client(client).build()

    @Provides
    @Named(NameKeys.OKHTTP_AUTHENTICATED)
    fun okHttpClient(okHttpClientBuilder: OkHttpClient.Builder, authToken: Auth.Token) =
        okHttpClientBuilder.withToken(authToken)

    @Provides
    @Named(NameKeys.SERVICE_AUTHENTICATED)
    fun musicBotService(@Named(NameKeys.RETROFIT_AUTHENTICATED) retrofit: Retrofit): MusicBotService =
        retrofit.create(MusicBotService::class.java)
}