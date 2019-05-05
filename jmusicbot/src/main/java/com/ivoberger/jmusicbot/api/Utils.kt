package com.ivoberger.jmusicbot.api

import android.net.wifi.WifiManager
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.exceptions.*
import com.ivoberger.jmusicbot.model.Auth
import com.ivoberger.jmusicbot.model.Event
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

private const val GROUP_ADDRESS = "224.0.0.142"
internal const val PORT = 42945
private const val LOCK_TAG = "enq_broadcast"

internal fun WifiManager.discoverHost(): String? {

    val lock = createMulticastLock(LOCK_TAG)
    if (lock.isHeld) return null
    lock.acquire()
    Timber.d("Multicast lock acquired")
    return try {
        MulticastSocket(PORT).use { socket ->
            val groupAddress = InetAddress.getByName(GROUP_ADDRESS)
            socket.joinGroup(groupAddress)
            socket.soTimeout = 4000
            val buffer = ByteArray(8)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.broadcast = true
            socket.receive(packet)
            socket.leaveGroup(groupAddress)
            packet.address.hostAddress
        }
    } catch (e: IOException) {
        null
    } finally {
        lock.release()
    }
}

internal fun OkHttpClient.Builder.withToken(token: Auth.Token) = addInterceptor { chain ->
    chain.proceed(chain.request().newBuilder().header(KEY_AUTHORIZATION, token.toAuthHeader()).build())
}.authenticator(TokenAuthenticator()).build()


@Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class, ServerErrorException::class)
internal suspend inline fun <reified T> Deferred<Response<T>>.process(
    successCodes: List<Int> = listOf(200, 201, 204),
    errorCodes: Map<Int, Exception> = mapOf(),
    notFoundType: NotFoundException.Type = NotFoundException.Type.SONG,
    invalidParamsType: InvalidParametersException.Type = InvalidParametersException.Type.MISSING
): T? {
    val response: Response<T>
    try {
        response = await()
    } catch (e: IOException) {
        JMusicBot.stateMachine.transition(Event.OnDisconnect(e))
        throw e
    }
    return when (response.code()) {
        in successCodes -> response.body()
        in errorCodes -> throw errorCodes.getValue(response.code())
        400 -> throw InvalidParametersException(
            invalidParamsType,
            response.errorBody()!!.string()
        )
        401 -> throw AuthException(
            AuthException.Reason.NEEDS_AUTH,
            response.errorBody()!!.string()
        )
        403 -> throw AuthException(
            AuthException.Reason.NEEDS_PERMISSION,
            response.errorBody()!!.string()
        )
        404 -> throw NotFoundException(notFoundType, response.errorBody()!!.string())
        409 -> throw UsernameTakenException()
        else -> {
            Timber.e("Server Error: ${response.errorBody()!!.string()}, ${response.code()}")
            throw ServerErrorException(response.code())
        }
    }
}
