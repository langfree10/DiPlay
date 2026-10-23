package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.media.ElantraAudioCompatibility
import java.io.ByteArrayOutputStream

/** Hyundai website artwork fitted into CarPlay's existing square car-button format. */
internal object ElantraCarIcon {
    fun defaultBytes(context: Context): ByteArray = if (ElantraAudioCompatibility.supported()) {
        val bitmap = preview(context)
        try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
    } else context.resources.openRawResource(R.raw.ic_car_home).use { it.readBytes() }

    fun preview(context: Context): Bitmap {
        if (!ElantraAudioCompatibility.supported())
            return BitmapFactory.decodeResource(context.resources, R.raw.placeholder_icon)
        val source = BitmapFactory.decodeResource(context.resources, R.raw.hyundai_wordmark,
            BitmapFactory.Options().apply { inScaled = false })
        val target = Bitmap.createBitmap(AirPlayIconSizing.MAX_PIXELS, AirPlayIconSizing.MAX_PIXELS, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        canvas.drawColor(Color.WHITE)
        // Oval emblem in the first 84x44 pixels of Hyundai's 310x44 official wordmark.
        canvas.drawBitmap(source, Rect(0, 0, 84, 44), RectF(20f, 56f, 172f, 136f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        source.recycle()
        return target
    }
}
