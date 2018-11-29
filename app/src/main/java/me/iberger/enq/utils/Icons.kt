package me.iberger.enq.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.Size
import androidx.core.content.ContextCompat.getColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon


fun IIcon.make(context: Context): IconicsDrawable = IconicsDrawable(context, this)
fun IIcon.make(context: Context, @ColorRes color: Int): IconicsDrawable =
    make(context).color(getColor(context, color))

fun IIcon.make(context: Context, @ColorRes color: Int, @Size sizeDp: Int): IconicsDrawable =
    make(context, color).sizeDp(sizeDp)

fun IIcon.makeBitmap(context: Context) = make(context).toBitmap()
fun IIcon.makeBitmap(context: Context, @ColorRes color: Int) = make(context, color).toBitmap()
fun IIcon.makeBitmap(context: Context, @ColorRes color: Int, @Size sizeDp: Int) =
    make(context, color, sizeDp).toBitmap()