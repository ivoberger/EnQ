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

import android.util.Base64
import com.auth0.android.jwt.JWT
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

sealed class Auth {

    @JsonClass(generateAdapter = true)
    class Register(
        @Json(name = "name") val name: String,
        @Json(name = "userId") val uuid: String = UUID.randomUUID().toString()
    ) : Auth() {
        constructor(user: User) : this(user.name, user.id)
    }

    class Basic(val name: String, val password: String) : Auth() {
        constructor(user: User) : this(
            user.name,
            user.password ?: throw InvalidParametersException(
                InvalidParametersException.Type.INVALID_PASSWORD,
                "Password can't be null"
            )
        )

        fun toAuthHeader(): String =
            "Basic ${Base64.encodeToString("$name:$password".toByteArray(), Base64.DEFAULT).trim()}"
    }

    class Token(private val token: String) : Auth() {

        private val jwt by lazy { JWT(token) }
        val permissions by lazy { Permissions.fromClaims(jwt.claims) }

        override fun toString(): String = token
        fun toAuthHeader() = "Bearer $token"
        fun isExpired() = jwt.isExpired(60)
    }

    @JsonClass(generateAdapter = true)
    class PasswordChange(@Json(name = "newPassword") val newPassword: String) : Auth()
}

@JsonClass(generateAdapter = true)
data class AuthExpectation(val format: AuthType, val type: String?, val permissions: List<Permissions>?)

enum class AuthType {
    @Json(name = "Basic")
    BASIC,
    @Json(name = "Token")
    TOKEN
}
