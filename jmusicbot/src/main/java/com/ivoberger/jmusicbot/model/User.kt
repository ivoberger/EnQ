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

import com.ivoberger.jmusicbot.JMusicBot
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class User(
    @Json(name = "name") val name: String,
    password: String? = null,
    @Json(name = "userId") val id: String?,
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
    }

    override fun toString(): String = mUserAdapter.toJson(this@User)
}
