package uk.co.jakelee.apodwallpaper.helper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import timber.log.Timber
import java.io.File

class WallpaperHelper(val context: Context, val prefHelper: PreferenceHelper) {
    val manager = WallpaperManager.getInstance(context)

    fun updateWallpaper(bitmap: Bitmap) = manager.setBitmap(bitmap)

    fun updateLockScreen(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.setStream(file.inputStream(), null, false, WallpaperManager.FLAG_LOCK)
        } else {
            Timber.i("Can't set lock screen!")
        }
    }

    fun applyRequired(dateString: String) {
        applyRequired(dateString, FileSystemHelper(context).getImage(dateString))
    }

    fun applyRequired(dateString: String, image: Bitmap) {
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.wallpaper_enabled)) {
            updateWallpaper(image)
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.lockscreen_enabled)) {
            updateLockScreen(FileSystemHelper(context).getImagePath(dateString))
        }
    }
}