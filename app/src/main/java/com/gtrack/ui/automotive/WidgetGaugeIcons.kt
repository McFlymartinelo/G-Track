package com.gtrack.ui.automotive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

internal fun loadGaugeIconBitmap(
    context: Context,
    @DrawableRes resId: Int,
    sizePx: Int = 96
): ImageBitmap? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val wrapped = DrawableCompat.wrap(drawable).mutate()
    DrawableCompat.setTint(wrapped, android.graphics.Color.WHITE)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    wrapped.setBounds(0, 0, sizePx, sizePx)
    wrapped.draw(canvas)
    return bitmap.asImageBitmap()
}
