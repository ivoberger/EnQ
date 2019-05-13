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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerState(
    @Json(name = "state") val state: PlayerStates,
    @Json(name = "songEntry") val songEntry: SongEntry?,
    @Json(name = "progress") val progress: Int = 0
) {
    constructor(state: PlayerStates) : this(state, null)
}

@JsonClass(generateAdapter = true)
data class PlayerStateChange(
    @Json(name = "action") val action: PlayerAction
)

enum class PlayerStates {
    PLAY, PAUSE, STOP, ERROR
}

enum class PlayerAction {
    PLAY, PAUSE, SKIP
}
