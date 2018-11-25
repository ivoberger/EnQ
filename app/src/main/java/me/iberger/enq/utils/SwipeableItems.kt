package me.iberger.enq.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeDragCallback
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon

fun setupSwipeActions(
    context: Context,
    recyclerView: RecyclerView,
    itemSwipeCallback: SimpleSwipeCallback.ItemSwipeCallback,
    iconLeft: IIcon, @ColorRes colorLeft: Int,
    iconRight: IIcon, @ColorRes colorRight: Int
) {
    val leaveBehindDrawableRight =
        IconicsDrawable(context, iconRight).color(Color.WHITE).sizeDp(24)
    val leaveBehindDrawableLeft =
        IconicsDrawable(context, iconLeft).color(Color.WHITE).sizeDp(24)
    val touchCallback = SimpleSwipeCallback(
        itemSwipeCallback,
        leaveBehindDrawableLeft,
        ItemTouchHelper.LEFT,
        ContextCompat.getColor(context, colorLeft)
    )
        .withBackgroundSwipeRight(ContextCompat.getColor(context, colorRight))
        .withLeaveBehindSwipeRight(leaveBehindDrawableRight)
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
    val leaveBehindDrawableRight =
        IconicsDrawable(context, iconRight).color(Color.WHITE).sizeDp(24)
    val leaveBehindDrawableLeft =
        IconicsDrawable(context, iconLeft).color(Color.WHITE).sizeDp(24)
    val touchCallback = SimpleSwipeDragCallback(
        itemTouchCallback,
        itemSwipeCallback,
        leaveBehindDrawableLeft,
        ItemTouchHelper.LEFT,
        ContextCompat.getColor(context, colorLeft)
    )
        .withBackgroundSwipeRight(ContextCompat.getColor(context, colorRight))
        .withLeaveBehindSwipeRight(leaveBehindDrawableRight)
    ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
}