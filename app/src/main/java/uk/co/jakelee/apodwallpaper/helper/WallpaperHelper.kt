package uk.co.jakelee.apodwallpaper.helper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import timber.log.Timber
import java.io.File

class WallpaperHelper(context: Context) {
    val manager = WallpaperManager.getInstance(context)

    fun updateWallpaper(bitmap: Bitmap) = manager.setBitmap(bitmap)

    fun updateLockScreen(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.setStream(file.inputStream(), null, false, WallpaperManager.FLAG_LOCK)
        } else {
            Timber.i("Can't set lock screen!")
        }
    }
}