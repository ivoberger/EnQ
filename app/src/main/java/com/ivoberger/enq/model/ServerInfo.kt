package com.ivoberger.enq.model

import android.os.Parcelable
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.jmusicbot.model.VersionInfo
import com.squareup.moshi.Types
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import java.lang.reflect.Type

@Parcelize
@TypeParceler<VersionInfo, AppSettings.VersionInfoParceler>()
data class ServerInfo(
    val baseUrl: String, val versionInfo: VersionInfo
) : Parcelable {
    companion object {
        val listMoshiType: Type = Types.newParameterizedType(List::class.java, ServerInfo::class.java)
    }
}