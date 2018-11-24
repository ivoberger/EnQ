package me.iberger.jmusicbot.network

import me.iberger.jmusicbot.*
import me.iberger.jmusicbot.data.*
import retrofit2.Call
import retrofit2.http.*

private const val URL_USER = "user"
private const val URL_PLAYER = "player"
private const val URL_SUGGEST = "suggester"
private const val URL_PROVIDER = "provider"
private const val URL_QUEUE = "queue"

internal interface MusicBotAPI {
    // User operations
    @PUT(URL_USER)
    fun changePassword(@Body newPassword: Credentials.PasswordChange): Call<String>

    @DELETE(URL_USER)
    fun deleteUser(): Call<Unit>

    @POST(URL_USER)
    fun registerUser(@Body credentials: Credentials.Register): Call<String>

    @PUT("token")
    fun login(@Body loginCredentials: Credentials.Login): Call<String>

    @GET(URL_PLAYER)
    fun attemptLogin(@Header(KEY_AUTHORIZATION) authToken: String): Call<PlayerState>

    // Song operations

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}/{$KEY_SONG_ID}")
    fun lookupSong(@Path(KEY_PROVIDER_ID) providerId: String, @Path(KEY_SONG_ID) songId: String): Call<Song>

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}")
    fun searchForSong(@Path(KEY_PROVIDER_ID) providerId: String, @Query(KEY_QUERY) query: String): Call<List<Song>>

    // Queue operations

    @DELETE("$URL_PLAYER/$URL_QUEUE")
    fun dequeue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Call<List<QueueEntry>>

    @PUT("$URL_PLAYER/$URL_QUEUE")
    fun enqueue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Call<List<QueueEntry>>

    @PUT("$URL_PLAYER/$URL_QUEUE/order")
    fun moveSong(@Body song: Song, @Query("index") index: Int): Call<List<QueueEntry>>

    @GET("$URL_PLAYER/$URL_QUEUE")
    fun getQueue(): Call<List<QueueEntry>>

    @GET("$URL_PLAYER/$URL_QUEUE/history")
    fun getHistory(): Call<List<QueueEntry>>

    // Suggest operations

    @GET(URL_SUGGEST)
    fun getSuggesters(): Call<List<MusicBotPlugin>>

    @DELETE("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    fun deleteSuggestion(
        @Path(KEY_SUGGESTER_ID) suggesterId: String,
        @Query(KEY_SONG_ID) songId: String,
        @Query(KEY_PROVIDER_ID) providerId: String
    ): Call<Unit>

    @GET("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    fun getSuggestions(@Path(KEY_SUGGESTER_ID) suggesterId: String, @Query("max") limit: Int = 32): Call<List<Song>>

    // Provider operations

    @GET(URL_PROVIDER)
    fun getProvider(): Call<List<MusicBotPlugin>>

    // Player operations

    @GET(URL_PLAYER)
    fun getPlayerState(): Call<PlayerState>

    @PUT(URL_PLAYER)
    fun setPlayerState(@Body playerStateChange: PlayerStateChange): Call<PlayerState>

    @PUT("$URL_PLAYER/pause")
    fun pause(): Call<PlayerState>

    @PUT("$URL_PLAYER/play")
    fun play(): Call<PlayerState>

    @PUT("$URL_PLAYER/next")
    fun skip(): Call<PlayerState>
}