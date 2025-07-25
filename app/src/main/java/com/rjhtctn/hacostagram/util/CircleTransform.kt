package com.rjhtctn.hacostagram.util

import android.graphics.*
import com.squareup.picasso.Transformation
import kotlin.math.min
import androidx.core.graphics.createBitmap

class CircleTransform : Transformation {
    override fun key(): String = "circle"

    override fun transform(source: Bitmap): Bitmap {
        val size = min(source.width, source.height)
        val x = (source.width  - size) / 2
        val y = (source.height - size) / 2

        val squared = Bitmap.createBitmap(source, x, y, size, size)
        if (squared != source) source.recycle()

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)
        squared.recycle()
        return bitmap
    }
}