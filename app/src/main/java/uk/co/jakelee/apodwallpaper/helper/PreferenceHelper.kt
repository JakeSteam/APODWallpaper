package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed

class PreferenceHelper(context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    enum class StringPref { Title, Desc, Image, LastCheckedDate }

    fun saveApodData(response: ResponseApodProcessed) = prefs.edit()
        .putString("${response.date}_${StringPref.Title.name}", response.title)
        .putString("${response.date}_${StringPref.Desc.name}", response.desc)
        .putString("${response.date}_${StringPref.Image.name}", response.imageUrl)
        .apply()

    fun getApodData(fsh: FileSystemHelper, date: String) = ResponseApodProcessed(
            date,
            prefs.getString("${date}_${StringPref.Title.name}", ""),
            prefs.getString("${date}_${StringPref.Desc.name}", ""),
            prefs.getString("${date}_${StringPref.Image.name}", ""),
            BitmapFactory.decodeFile(fsh.getImage(date).path)
        )

    fun doesDataExist(context: Context, date: String) = FileSystemHelper(context).getImage(date).exists()

    fun updateLastPulledDate(date: String) = prefs.edit()
        .putString("last_pulled", date)
        .apply()

    fun getLastPulledDate() = prefs.getString("last_pulled", "")

    fun updateLastCheckedDate() = prefs.edit()
        .putLong("last_checked", System.currentTimeMillis())
        .apply()

    fun getLastCheckedDate() = prefs.getLong("last_checked", 0)

    fun setScheduledTask() = prefs.edit()
        .putBoolean("scheduled_tasks", true)
        .apply()

    fun haveScheduledTask() = prefs.getBoolean("scheduled_tasks", false)
}