package com.ivoberger.jmusicbot.di

import com.ivoberger.jmusicbot.api.MusicBotService
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [ServerModule::class])
internal interface ServerSession {
    @Named(NameKeys.BASE_URL)
    fun baseUrl(): String

    @Named(NameKeys.SERVICE_BASE)
    fun musicBotService(): MusicBotService

    fun userSession(userModule: UserModule): UserSession
}