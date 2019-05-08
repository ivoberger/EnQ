package com.ivoberger.enq.model

import com.ivoberger.jmusicbot.model.VersionInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import java.lang.reflect.Type

@JsonClass(generateAdapter = true)
data class ServerInfo(
    @Json(name = "baseUrl") val baseUrl: String,
    @Json(name = "versionInfo") val versionInfo: VersionInfo
) {
    companion object {
        val listMoshiType: Type = Types.newParameterizedType(List::class.java, ServerInfo::class.java)
    }
}