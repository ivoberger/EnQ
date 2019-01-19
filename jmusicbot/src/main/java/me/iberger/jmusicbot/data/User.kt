package me.iberger.jmusicbot.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import me.iberger.jmusicbot.KEY_USER
import timber.log.Timber
import java.util.*

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "name") val name: String,
    @Json(name = "uuid") val uuid: String = UUID.randomUUID().toString(),
    @Json(name = "password") var password: String? = null
) {
    init {
        Timber.d("Creating User $this")
    }

    companion object {
        fun load(sharedPreferences: SharedPreferences, moshi: Moshi = Moshi.Builder().build()): User? {
            val userAdapter = moshi.adapter<User>(User::class.java)
            sharedPreferences.getString(KEY_USER, null)?.let {
                Timber.d("Creating user from json: $it")
                return userAdapter.fromJson(it) ?: throw JsonEncodingException("User data corrupted")
            }
            return null
        }
    }

    fun save(sharedPreferences: SharedPreferences) {
        val userAdapter = Moshi.Builder().build().adapter<User>(this::class.java)
        sharedPreferences.edit {
            putString(KEY_USER, userAdapter.toJson(this@User))
        }
        Timber.d("Saved user $this")
    }
}
