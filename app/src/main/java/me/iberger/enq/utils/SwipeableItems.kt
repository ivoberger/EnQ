package me.iberger.enq.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeDragCallback
import com.mikepenz.iconics.typeface.IIcon
import me.iberger.enq.R

fun setupSwipeActions(
    context: Context,
    recyclerView: RecyclerView,
    itemSwipeCallback: SimpleSwipeCallback.ItemSwipeCallback,
    iconLeft: IIcon, @ColorRes colorLeft: Int,
    iconRight: IIcon, @ColorRes colorRight: Int
) {
    val drawableRight = iconRight.make(context, R.color.white, 24)
    val drawableLeft = iconLeft.make(context, R.color.white, 24)
    val touchCallback = SimpleSwipeCallback(
        itemSwipeCallback, drawableLeft, ItemTouchHelper.LEFT, getColor(context, colorLeft)
    )
        .withBackgroundSwipeRight(getColor(context, colorRight))
        .withLeaveBehindSwipeRight(drawableRight)
    ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
}

fun setupSwipeDragActions(
    context: Context,
    recyclerView: RecyclerView,
    itemSwipeCallback: SimpleSwipeCallback.ItemSwipeCallback,
    itemTouchCallback: ItemTouchCallback,
    iconLeft: IIcon, @ColorRes colorLeft: Int,
    iconRight: IIcon, @ColorRes colorRight: Int
) {
    val drawableRight = iconRight.make(context, R.color.white, 24)
    val drawableLeft = iconLeft.make(context, R.color.white, 24)
    val touchCallback = SimpleSwipeDragCallback(
        itemTouchCallback, itemSwipeCallback,
        drawableLeft, ItemTouchHelper.LEFT, getColor(context, colorLeft)
    )
        .withBackgroundSwipeRight(getColor(context, colorRight))
        .withLeaveBehindSwipeRight(drawableRight)
    ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
}