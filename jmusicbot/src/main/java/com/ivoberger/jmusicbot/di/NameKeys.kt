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
package com.ivoberger.jmusicbot.di

internal object NameKeys {
    const val BASE_URL = "baseUrl"

    const val BUILDER_RETROFIT_BASE = "basicRetrofitBuilder"
    const val BUILDER_RETROFIT_URL = "urlRetrofitBuilder"

    const val RETROFIT_AUTHENTICATED = "authenticatedRetrofit"

    const val OKHTTP_BASE = "basicOkHttp"
    const val OKHTTP_AUTHENTICATED = "authenticatedOkHttp"

    const val SERVICE_BASE = "baseService"
    const val SERVICE_AUTHENTICATED = "authenticatedService"
}
