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
package com.ivoberger.jmusicbot.api

import com.ivoberger.jmusicbot.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.KEY_PROVIDER_ID
import com.ivoberger.jmusicbot.KEY_QUERY
import com.ivoberger.jmusicbot.KEY_SONG_ID
import com.ivoberger.jmusicbot.KEY_SUGGESTER_ID
import com.ivoberger.jmusicbot.model.Auth
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import com.ivoberger.jmusicbot.model.PlayerAction
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.PlayerStateChange
import com.ivoberger.jmusicbot.model.QueueEntry
import com.ivoberger.jmusicbot.model.Song
import com.ivoberger.jmusicbot.model.User
import com.ivoberger.jmusicbot.model.VersionInfo
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MusicBotService {
    companion object {
        private const val URL_USER = "user"
        private const val URL_PLAYER = "player"
        private const val URL_SUGGEST = "suggester"
        private const val URL_PROVIDER = "provider"
        private const val URL_QUEUE = "queue"
    }

    // User operations
    @PUT(URL_USER)
    fun changePassword(@Body newPassword: Auth.PasswordChange): Deferred<Response<String>>

    @DELETE(URL_USER)
    fun deleteUser(): Deferred<Response<Unit>>

    @POST(URL_USER)
    fun registerUser(@Body credentials: Auth.Register): Deferred<Response<String>>

    @GET("token")
    fun loginUser(@Header(KEY_AUTHORIZATION) loginCredentials: String): Deferred<Response<String>>

    @GET(URL_USER)
    fun testToken(
        @Header(KEY_AUTHORIZATION) authToken: String
    ): Deferred<Response<User>>

    // Song operations

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}/{$KEY_SONG_ID}")
    fun lookupSong(@Path(KEY_PROVIDER_ID) providerId: String, @Path(KEY_SONG_ID) songId: String): Deferred<Response<Song>>

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}")
    fun searchForSong(@Path(KEY_PROVIDER_ID) providerId: String, @Query(KEY_QUERY) query: String): Deferred<Response<List<Song>>>

    // Queue operations

    @DELETE("$URL_PLAYER/$URL_QUEUE")
    fun dequeue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Deferred<Response<List<QueueEntry>>>

    @PUT("$URL_PLAYER/$URL_QUEUE")
    fun enqueue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Deferred<Response<List<QueueEntry>>>

    @PUT("$URL_PLAYER/$URL_QUEUE/order")
    fun moveEntry(
        @Body entry: QueueEntry,
        @Query("providerId") providerId: String,
        @Query("songId") songId: String,
        @Query("index") index: Int
    ): Deferred<Response<List<QueueEntry>>>

    @GET("$URL_PLAYER/$URL_QUEUE")
    fun getQueue(): Deferred<Response<List<QueueEntry>>>

    @GET("$URL_PLAYER/$URL_QUEUE/history")
    fun getHistory(): Deferred<Response<List<QueueEntry>>>

    // Suggest operations

    @GET(URL_SUGGEST)
    fun getSuggesters(): Deferred<Response<List<MusicBotPlugin>>>

    @DELETE("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    fun deleteSuggestion(
        @Path(KEY_SUGGESTER_ID) suggesterId: String,
        @Query(KEY_SONG_ID) songId: String,
        @Query(KEY_PROVIDER_ID) providerId: String
    ): Deferred<Response<Unit>>

    @GET("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    fun getSuggestions(@Path(KEY_SUGGESTER_ID) suggesterId: String, @Query("max") limit: Int = 32): Deferred<Response<List<Song>>>

    // Provider operations

    @GET(URL_PROVIDER)
    fun getProvider(): Deferred<Response<List<MusicBotPlugin>>>

    // Player operations

    @GET(URL_PLAYER)
    fun getPlayerState(): Deferred<Response<PlayerState>>

//    @PUT(URL_PLAYER)
//    fun setPlayerState(@Body playerStateChange: PlayerStateChange): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun pause(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.PAUSE)): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun play(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.PLAY)): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun skip(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.SKIP)): Deferred<Response<PlayerState>>

    @GET("version")
    fun getVersionInfo(): Deferred<Response<VersionInfo>>
}
