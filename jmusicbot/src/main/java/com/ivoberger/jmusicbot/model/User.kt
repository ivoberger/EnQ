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
package com.ivoberger.jmusicbot.model

import android.content.SharedPreferences
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_USER
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.util.UUID

@JsonClass(generateAdapter = true)
class User(
    @Json(name = "name") val name: String,
    password: String? = null,
    @Json(name = "userId") val id: String = UUID.randomUUID().toString(),
    @Json(name = "permissions") permissions: List<Permissions> = listOf()
) {

    @Json(name = "password")
    var password: String? = password
        set(value) {
            field = value
            JMusicBot.user = this
        }

    @Json(name = "permissions")
    var permissions: List<Permissions> = permissions
        set(value) {
            field = value
            JMusicBot.user = this
        }

    companion object {
        private val mUserAdapter by lazy { JMusicBot.mBaseComponent.moshi.adapter<User>(User::class.java) }
        fun fromString(serializedUser: String) = mUserAdapter.fromJson(serializedUser)

        fun load(sharedPreferences: SharedPreferences, moshi: Moshi = Moshi.Builder().build()): User? {
            val userAdapter = moshi.adapter<User>(User::class.java)
            sharedPreferences.getString(KEY_USER, null)?.let {
                Timber.d("Creating user from json: $it")
                return userAdapter.fromJson(it)
                    ?: throw JsonEncodingException("User data corrupted")
            }
            return null
        }
    }

    override fun toString(): String = mUserAdapter.toJson(this@User)
}
