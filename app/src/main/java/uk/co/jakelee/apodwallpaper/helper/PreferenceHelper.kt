package uk.co.jakelee.apodwallpaper

import android.content.Context
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed

class PreferenceHelper(context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun saveResponsePrefs(response: ResponseApodProcessed) = prefs.edit()
        .putString("${response.date}_title", response.title)
        .putString("${response.date}_desc", response.desc)
        .putString("${response.date}_image", response.imageUrl)
        .apply()

    fun updateLastCheckDate() = prefs.edit()
        .putLong("last_checked", System.currentTimeMillis())
        .apply()

    fun getLastCheckDate() = prefs.edit()
        .putLong("last_checked", System.currentTimeMillis())
        .apply()
}