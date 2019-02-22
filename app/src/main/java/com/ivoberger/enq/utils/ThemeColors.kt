package com.ivoberger.enq.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import com.ivoberger.enq.R
import splitties.resources.color
import timber.log.Timber


fun Context.attributeColor(attributeId: Int): Int {
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
fun Context.secondaryColor() = attributeColor(R.attr.colorSecondary)
fun Context.onSecondaryColor() = attributeColor(R.attr.colorOnSecondary)
