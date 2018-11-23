package me.iberger.jmusicbot.network

import android.content.Context
import android.net.wifi.WifiManager
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.ServerErrorException
import retrofit2.Call
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

private const val GROUP_ADDRESS = "224.0.0.142"
private const val PORT = 42945
private const val LOCK_TAG = "enq_broadcast"

internal fun verifyHostAddress(context: Context, address: String? = null) {
    MusicBot.baseUrl = address ?: MusicBot.baseUrl ?: discoverHost(context)
    Timber.d("New host address: ${MusicBot.baseUrl}")
}

private fun discoverHost(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val lock = wifiManager.createMulticastLock(LOCK_TAG)
    lock.acquire()
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
            "http://${packet.address.hostAddress}:$PORT/v1/"
        }
    } catch (e: IOException) {
        null
    } finally {
        lock.release()
    }
}

internal fun <T> Call<T>.process(
    successCodes: List<Int> = listOf(200, 201, 204),
    errorCodes: Map<Int, Exception> = mapOf(),
    notFoundType: NotFoundException.Type = NotFoundException.Type.SONG,
    invalidParamsType: InvalidParametersException.Type = InvalidParametersException.Type.MISSING
): T? {
    val response = execute()
    return when (response.code()) {
        in successCodes -> response.body()
        in errorCodes -> throw errorCodes[response.code()]!!
        400 -> throw InvalidParametersException(invalidParamsType, response.errorBody()!!.string())
        401 -> throw AuthException(AuthException.Reason.NEEDS_AUTH, response.errorBody()!!.string())
        403 -> throw AuthException(AuthException.Reason.NEEDS_PERMISSION, response.errorBody()!!.string())
        404 -> throw NotFoundException(notFoundType, response.errorBody()!!.string())
        else -> {
            Timber.e("Error: ${response.errorBody()!!.string()}")
            throw ServerErrorException(response.code())
        }
    }
}