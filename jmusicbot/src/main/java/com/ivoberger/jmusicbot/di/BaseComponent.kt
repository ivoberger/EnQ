package com.ivoberger.jmusicbot.di

import android.net.wifi.WifiManager
import com.squareup.moshi.Moshi
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [BaseModule::class])
internal interface BaseComponent {
    val wifiManager: WifiManager
    val moshi: Moshi
    fun serverSession(serverModule: ServerModule): ServerSession
}