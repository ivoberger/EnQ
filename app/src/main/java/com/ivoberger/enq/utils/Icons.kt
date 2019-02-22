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
