package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed

class PreferenceHelper(val context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    enum class StringPref { Title, Desc, Image, ImageHd, Copyright, LastCheckedDate }

    fun saveApodData(response: ResponseApodProcessed) = prefs.edit()
        .putString("${response.date}_${StringPref.Title.name}", response.title)
        .putString("${response.date}_${StringPref.Desc.name}", response.desc)
        .putString("${response.date}_${StringPref.Image.name}", response.imageUrl)
        .putString("${response.date}_${StringPref.ImageHd.name}", response.imageUrlHd)
        .putString("${response.date}_${StringPref.Copyright.name}", response.copyright)
        .apply()

    fun getApodData(fsh: FileSystemHelper, date: String) = ResponseApodProcessed(
            date,
            prefs.getString("${date}_${StringPref.Title.name}", "")!!,
            prefs.getString("${date}_${StringPref.Desc.name}", "")!!,
            prefs.getString("${date}_${StringPref.Image.name}", "")!!,
            prefs.getString("${date}_${StringPref.ImageHd.name}", "")!!,
            prefs.getString("${date}_${StringPref.Copyright.name}", "")!!,
            BitmapFactory.decodeFile(fsh.getImage(date).path)
        )

    fun doesDataExist(context: Context, date: String) = FileSystemHelper(context).getImage(date).exists()

    fun updateLastPulledDateString(date: String) = prefs.edit()
        .putString("last_pulled", date)
        .apply()

    fun getLastPulledDateString() = prefs.getString("last_pulled", "")!!

    fun updateLastCheckedDate() = prefs.edit()
        .putLong("last_checked", System.currentTimeMillis())
        .apply()

    fun getLastCheckedDate() = prefs.getLong("last_checked", 0)

    fun updateLastRunDate(manual: Boolean) = prefs.edit()
        .putLong(getLastRunPref(manual), System.currentTimeMillis())
        .apply()
    fun getLastRunDate(manual: Boolean) = prefs.getLong(getLastRunPref(manual), 0)
    private fun getLastRunPref(manual: Boolean) = context.getString(if (manual) R.string.last_manual_run else R.string.last_automatic_run)

    fun updateLastSetDate(manual: Boolean) = prefs.edit()
        .putLong(getLastSetPref(manual), System.currentTimeMillis())
        .apply()
    fun getLastSetDate(manual: Boolean) = prefs.getLong(getLastSetPref(manual), 0)
    private fun getLastSetPref(manual: Boolean) = context.getString(if (manual) R.string.last_manual_set else R.string.last_automatic_set)

    fun haveScheduledTask() = false
}