package me.iberger.jmusicbot.model

import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.KEY_USER
import timber.log.Timber
import java.util.*

@JsonClass(generateAdapter = true)
class User(
    @Json(name = "name") val name: String,
    password: String? = null,
    @Json(name = "uuid") val uuid: String = UUID.randomUUID().toString(),
    @Json(name = "permissions") var permissions: MutableList<Permissions> = mutableListOf()
) {

    @Json(name = "password")
    var password: String? = password
        set(value) {
            field = value
            BotPreferences.user = this
        }

    init {
        Timber.d("Creating User $this")
    }

    companion object {
        private val mUserAdapter by lazy { JMusicBot.mMoshi.adapter<User>(User::class.java) }
        fun fromString(serializedUser: String) = mUserAdapter.fromJson(serializedUser)

        fun load(sharedPreferences: SharedPreferences, moshi: Moshi = Moshi.Builder().build()): User? {
            val userAdapter = moshi.adapter<User>(User::class.java)
            sharedPreferences.getString(KEY_USER, null)?.let {
                Timber.d("Creating user from json: $it")
                return userAdapter.fromJson(it) ?: throw JsonEncodingException("User data corrupted")
            }
            return null
        }
    }

    fun save(sharedPreferences: SharedPreferences, moshi: Moshi = Moshi.Builder().build()) {
        val userAdapter = moshi.adapter<User>(this::class.java)
        sharedPreferences.edit {
            putString(KEY_USER, userAdapter.toJson(this@User))
        }
        Timber.d("Saved user $this")
    }

    override fun toString(): String = mUserAdapter.toJson(this@User)
}
