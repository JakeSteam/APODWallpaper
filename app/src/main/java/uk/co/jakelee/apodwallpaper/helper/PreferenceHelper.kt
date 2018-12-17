package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed

class PreferenceHelper(val context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    enum class OldStringPref { Title, Desc, Image, ImageHd, Copyright, LastCheckedDate }

    fun saveApodData(response: ResponseApodProcessed) = prefs.edit()
        .putString("${response.date}_${OldStringPref.Title.name}", response.title)
        .putString("${response.date}_${OldStringPref.Desc.name}", response.desc)
        .putString("${response.date}_${OldStringPref.Image.name}", response.imageUrl)
        .putString("${response.date}_${OldStringPref.ImageHd.name}", response.imageUrlHd)
        .putString("${response.date}_${OldStringPref.Copyright.name}", response.copyright)
        .apply()

    fun getApodData(fsh: FileSystemHelper, date: String) = ResponseApodProcessed(
            date,
            prefs.getString("${date}_${OldStringPref.Title.name}", "")!!,
            prefs.getString("${date}_${OldStringPref.Desc.name}", "")!!,
            prefs.getString("${date}_${OldStringPref.Image.name}", "")!!,
            prefs.getString("${date}_${OldStringPref.ImageHd.name}", "")!!,
            prefs.getString("${date}_${OldStringPref.Copyright.name}", "")!!,
            BitmapFactory.decodeFile(fsh.getImage(date).path)
        )

    fun doesDataExist(context: Context, date: String) = FileSystemHelper(context).getImage(date).exists()

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

    enum class BooleanPref(val prefId: Int, val defaultId: Int) {
        automatic_enabled(R.string.automatic_enabled, R.bool.automatic_enabled_default),
        automatic_check_wifi(R.string.automatic_check_wifi, R.bool.automatic_check_wifi_default),
        show_description(R.string.show_description, R.bool.show_description_default)
    }
    fun getBooleanPref(pref: BooleanPref) = prefs.getBoolean(context.getString(pref.prefId), context.resources.getBoolean(pref.defaultId))
    fun setBooleanPref(pref: BooleanPref, value: Boolean) = prefs.edit().putBoolean(context.getString(pref.prefId), value).commit()

    enum class StringPref(val prefId: Int, val defaultId: Int) {
        last_pulled(R.string.last_pulled, R.string.empty_string)
    }
    fun getStringPref(pref: StringPref) = prefs.getString(context.getString(pref.prefId), context.getString(pref.defaultId))
    fun setStringPref(pref: StringPref, value: String) = prefs.edit().putString(context.getString(pref.prefId), value).commit()

    enum class LongPref(val prefId: Int, val defaultId: Int) {
        last_checked(R.string.last_checked, R.integer.empty_int),
    }
    fun getLongPref(pref: LongPref) = prefs.getLong(context.getString(pref.prefId), context.resources.getInteger(pref.defaultId).toLong())
    fun setLongPref(pref: LongPref, value: Long) = prefs.edit().putLong(context.getString(pref.prefId), value).commit()
}