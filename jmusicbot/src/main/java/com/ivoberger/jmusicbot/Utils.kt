package com.ivoberger.jmusicbot

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal fun getDefaultOkHttpClient() = OkHttpClient.Builder().cache(null).build()
internal fun getDefaultRetrofitClient(okHttpClient: OkHttpClient, baseUrl: String?) = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(JMusicBot.mMoshi).asLenient())
    .baseUrl(baseUrl ?: "http://localhost")
    .client(okHttpClient)
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()
