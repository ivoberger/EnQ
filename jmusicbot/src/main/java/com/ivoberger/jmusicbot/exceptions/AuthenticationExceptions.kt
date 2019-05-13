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
package com.ivoberger.jmusicbot.exceptions

class AuthException : Exception {
    val reason: Reason

    constructor(reason: Reason) {
        this.reason = reason
    }

    constructor(reason: Reason, message: String) : super(message) {
        this.reason = reason
    }

    enum class Reason {
        NEEDS_AUTH, NEEDS_PERMISSION
    }
}

class UsernameTakenException : Exception("Username already in use")

class ServerErrorException(code: Int) : Exception("Server error $code")
