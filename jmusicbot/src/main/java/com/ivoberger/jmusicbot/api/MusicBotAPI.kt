package com.ivoberger.jmusicbot.api

import com.ivoberger.jmusicbot.*
import com.ivoberger.jmusicbot.model.*
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

private const val URL_USER = "user"
private const val URL_PLAYER = "player"
private const val URL_SUGGEST = "suggester"
private const val URL_PROVIDER = "provider"
private const val URL_QUEUE = "queue"

internal interface MusicBotAPI {
    // User operations
    @PUT(URL_USER)
    fun changePassword(@Body newPassword: AuthTypes.PasswordChange): Deferred<Response<String>>

    @DELETE(URL_USER)
    fun deleteUser(): Deferred<Response<Unit>>

    @POST(URL_USER)
    fun registerUser(@Body credentials: AuthTypes.Register): Deferred<Response<String>>

    @GET("jwt")
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
    fun moveEntry(@Body entry: QueueEntry, @Query("index") index: Int): Deferred<Response<List<QueueEntry>>>

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

    @PUT(URL_PLAYER)
    fun setPlayerState(@Body playerStateChange: PlayerStateChange): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun pause(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.PAUSE)): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun play(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.PLAY)): Deferred<Response<PlayerState>>

    @PUT(URL_PLAYER)
    fun skip(@Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.SKIP)): Deferred<Response<PlayerState>>
}
