package com.ivoberger.jmusicbot.di

import com.ivoberger.jmusicbot.api.MusicBotService
import com.ivoberger.jmusicbot.model.AuthTypes
import com.ivoberger.jmusicbot.model.User
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [UserModule::class])
internal interface UserSession {
    val user: User
    val authToken: AuthTypes.Token

    @Named(NameKeys.SERVICE_AUTHENTICATED)
    fun musicBotService(): MusicBotService
}