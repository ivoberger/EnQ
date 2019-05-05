package com.ivoberger.jmusicbot.di

import com.ivoberger.jmusicbot.api.MusicBotService
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named

@Module
internal class ServerModule(private val baseUrl: String) {

    @Provides
    @Named(NameKeys.BASE_URL)
    fun baseUrl(): String = baseUrl

    @Provides
    @Named(NameKeys.BUILDER_RETROFIT_URL)
    fun retrofitBuilder(@Named(NameKeys.BUILDER_RETROFIT_BASE) retrofitBuilder: Retrofit.Builder, @Named(NameKeys.BASE_URL) baseUrl: String) =
        retrofitBuilder.baseUrl(baseUrl)

    @Provides
    @Named(NameKeys.SERVICE_BASE)
    fun musicBotService(@Named(NameKeys.BUILDER_RETROFIT_URL) retrofitBuilder: Retrofit.Builder): MusicBotService =
        retrofitBuilder.build().create(MusicBotService::class.java)
}