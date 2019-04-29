package com.ivoberger.jmusicbot.di

import android.net.wifi.WifiManager
import com.ivoberger.jmusicbot.api.TokenAuthenticator
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class BaseModule(private val logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC) {

    @Provides
    @Singleton
    fun wifiManager(): WifiManager = splitties.systemservices.wifiManager!!

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun okHttpClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder().cache(null).addInterceptor(
        HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { msg ->
            Timber.tag("BotSDKNetworking").v(msg)
        }).setLevel(logLevel)
    ).authenticator(TokenAuthenticator())

    @Provides
    @Singleton
    @Named(NameKeys.BUILDER_RETROFIT_BASE)
    fun retrofitBuilder(okHttpClientBuilder: OkHttpClient.Builder, moshi: Moshi): Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .client(okHttpClientBuilder.build())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
}