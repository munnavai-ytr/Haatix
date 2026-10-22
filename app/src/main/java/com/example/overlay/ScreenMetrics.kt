package com.example.overlay

import android.content.Context
import android.os.Build
import android.util.Size
import android.view.WindowManager

object ScreenMetrics {
    fun realSize(context: Context): Size {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            Size(b.width(), b.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            Size(dm.widthPixels, dm.heightPixels)
        }
    }
}
