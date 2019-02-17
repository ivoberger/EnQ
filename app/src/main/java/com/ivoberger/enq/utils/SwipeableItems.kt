package com.ivoberger.enq.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ivoberger.enq.R
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.iconics.typeface.IIcon
import splitties.resources.color

fun setupSwipeActions(
    context: Context,
    recyclerView: RecyclerView,
    itemSwipeCallback: SimpleSwipeCallback.ItemSwipeCallback,
    iconLeft: IIcon, @ColorRes colorLeft: Int,
    iconRight: IIcon? = null, @ColorRes colorRight: Int? = null
) {
    val drawableLeft = context.icon(iconLeft).color(context.color(R.color.white)).sizeDp(24)
    val touchCallback =
        SimpleSwipeCallback(itemSwipeCallback, drawableLeft, ItemTouchHelper.LEFT, context.color(colorLeft))

    if (iconRight != null && colorRight != null) {
        val drawableRight = context.icon(iconRight).color(context.color(R.color.white)).sizeDp(24)
        touchCallback.withBackgroundSwipeRight(context.color(colorRight))
            .withLeaveBehindSwipeRight(drawableRight)
    }
    ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
}
