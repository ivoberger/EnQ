package com.ivoberger.jmusicbot.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionInfo(val apiVersion: String, val implementation: ImplementationInfo) {
    @JsonClass(generateAdapter = true)
    data class ImplementationInfo(val name: String, val version: String, val projectInfo: String? = null)
}