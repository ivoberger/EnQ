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
import android.graphics.Bitmap
import android.view.View
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon

fun Context.icon(icon: IIcon): IconicsDrawable = IconicsDrawable(this, icon)

fun Context.icon(icon: String): IconicsDrawable = IconicsDrawable(this, icon)

fun Fragment.icon(icon: IIcon): IconicsDrawable = context!!.icon(icon)

fun Fragment.icon(icon: String): IconicsDrawable = context!!.icon(icon)

fun View.icon(icon: String): IconicsDrawable = context.icon(icon)

fun View.bitmap(icon: String) = icon(icon).bitmap()

fun Context.bitmap(icon: IIcon): Bitmap = icon(icon).bitmap()

fun Context.bitmap(icon: String): Bitmap = icon(icon).bitmap()

fun IconicsDrawable.bitmap() = toBitmap()
