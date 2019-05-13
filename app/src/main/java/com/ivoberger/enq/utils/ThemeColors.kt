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
package com.ivoberger.enq.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment
import com.ivoberger.enq.R
import splitties.resources.color
import timber.log.Timber

fun Fragment.attributeColor(@AttrRes attributeId: Int): Int = context!!.attributeColor(attributeId)

fun Context.attributeColor(@AttrRes attributeId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeId, typedValue, true)
    val colorRes = typedValue.resourceId
    var color = -1
    try {
        color = color(colorRes)
    } catch (e: Resources.NotFoundException) {
        Timber.w("Not found color resource by id: $colorRes")
    }
    return color
}

fun Context.primaryColor() = attributeColor(R.attr.colorPrimary)
fun Context.onPrimaryColor() = attributeColor(R.attr.colorOnPrimary)
fun Fragment.onPrimaryColor() = context!!.onPrimaryColor()
fun Context.secondaryColor() = attributeColor(R.attr.colorSecondary)
fun Context.onSecondaryColor() = attributeColor(R.attr.colorOnSecondary)
