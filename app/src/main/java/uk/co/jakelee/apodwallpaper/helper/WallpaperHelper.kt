package uk.co.jakelee.apodwallpaper.helper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import timber.log.Timber
import java.io.File

class WallpaperHelper(val context: Context, val prefHelper: PreferenceHelper) {
    val manager = WallpaperManager.getInstance(context)

    enum class FilterResponse { Success, MinWidth, MinHeight, Ratio }

    fun applyRequired(dateString: String, bypassFilters: Boolean) {
        applyRequired(dateString, FileSystemHelper(context).getImage(dateString), bypassFilters)
    }

    fun applyRequired(dateString: String, image: Bitmap, bypassFilters: Boolean) {
        val filterResponse = applyFilters(image)
        var displayOverrideMessage = false
        if (filterResponse != FilterResponse.Success) {
            if (bypassFilters) {
                displayOverrideMessage = true
            } else {
                prefHelper.setStringPref(PreferenceHelper.StringPref.last_filtered_date, dateString)
                prefHelper.setStringPref(PreferenceHelper.StringPref.last_filtered_reason, filterResponse.name)
                return
            }
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.wallpaper_enabled)) {
            updateWallpaper(image)
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.lockscreen_enabled)) {
            updateLockScreen(FileSystemHelper(context).getImagePath(dateString))
        }
        if (displayOverrideMessage) {
            Toast.makeText(context, "Image wouldn't usually be set due to ${filterResponse.name}, applying anyway!", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyFilters(image: Bitmap): FilterResponse {
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.filtering_enabled)) {
            if (image.height < prefHelper.getIntPref(PreferenceHelper.IntPref.minimum_height)) {
                return FilterResponse.MinHeight
            } else if (image.width < prefHelper.getIntPref(PreferenceHelper.IntPref.minimum_width)) {
                return FilterResponse.MinWidth
            } else if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.filtering_ratio_enabled)
                && (((image.width / image.height.toDouble()) * 100) > prefHelper.getIntPref(PreferenceHelper.IntPref.filtering_ratio))) {
                return FilterResponse.Ratio
            }
        }
        return FilterResponse.Success
    }

    fun updateWallpaper(bitmap: Bitmap) = manager.setBitmap(bitmap)

    fun updateLockScreen(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.setStream(file.inputStream(), null, false, WallpaperManager.FLAG_LOCK)
        } else {
            Timber.i("Can't set lock screen!")
        }
    }
}