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

import android.net.wifi.WifiManager
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
    fun okHttpClient(): OkHttpClient.Builder = OkHttpClient.Builder().cache(null).addInterceptor(
        HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { msg ->
            Timber.tag("BotSDKNetworking").v(msg)
        }).setLevel(logLevel)
    )

    @Provides
    @Named(NameKeys.BUILDER_RETROFIT_BASE)
    fun retrofitBuilder(okHttpClientBuilder: OkHttpClient.Builder, moshi: Moshi): Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .client(okHttpClientBuilder.build())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
}