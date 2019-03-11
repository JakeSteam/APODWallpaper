package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.R

class PreferenceHelper(val context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    enum class BooleanPref(val prefId: Int, val defaultId: Int) {
        automatic_enabled(R.string.pref_automatic_enabled, R.bool.automatic_enabled_default),
        automatic_check_wifi(R.string.pref_automatic_check_wifi, R.bool.automatic_check_wifi_default),
        automatic_check_fix(R.string.pref_automatic_check_fix, R.bool.automatic_check_fix_default),
        show_description(R.string.pref_show_description, R.bool.show_description_default),
        custom_key_enabled(R.string.pref_custom_key_enabled, R.bool.custom_key_enabled_default),
        wallpaper_enabled(R.string.pref_wallpaper_enabled, R.bool.wallpaper_enabled_default),
        lockscreen_enabled(R.string.pref_lockscreen_enabled, R.bool.lockscreen_enabled_default),
        notifications_enabled(R.string.pref_notifications_enabled, R.bool.notifications_enabled_default),
        notifications_led(R.string.pref_notifications_led, R.bool.notifications_led_default),
        notifications_sound(R.string.pref_notifications_sound, R.bool.notifications_sound_default),
        notifications_vibrate(R.string.pref_notifications_vibrate, R.bool.notifications_vibrate_default),
        notifications_preview(R.string.pref_notifications_preview, R.bool.notifications_preview_default),
        filtering_enabled(R.string.pref_filtering_enabled, R.bool.filtering_enabled_default),
        filtering_ratio_enabled(R.string.pref_filtering_ratio_enabled, R.bool.filtering_ratio_enabled_default),
        first_time_setup(R.string.pref_first_time_setup, R.bool.first_time_setup_default),
        use_hd_images(R.string.pref_use_hd_images, R.bool.use_hd_images_default),
        use_greyscale_images(R.string.pref_use_greyscale_images, R.bool.use_greyscale_images_default)
    }

    fun getBooleanPref(pref: BooleanPref) =
        prefs.getBoolean(context.getString(pref.prefId), context.resources.getBoolean(pref.defaultId))

    fun setBooleanPref(pref: BooleanPref, value: Boolean) =
        prefs.edit().putBoolean(context.getString(pref.prefId), value).commit()

    enum class StringPref(val prefId: Int, val defaultId: Int) {
        last_pulled(R.string.pref_last_pulled, R.string.empty_string),
        custom_key(R.string.pref_custom_key, R.string.custom_key_default),
        last_filtered_date(R.string.pref_last_filtered_date, R.string.custom_key_default),
        last_filtered_reason(R.string.pref_last_filtered_reason, R.string.custom_key_default),
        notification_colour(R.string.pref_notifications_colour, R.string.notifications_colour_default)
    }

    fun getStringPref(pref: StringPref) =
        prefs.getString(context.getString(pref.prefId), context.getString(pref.defaultId))!!

    fun setStringPref(pref: StringPref, value: String) =
        prefs.edit().putString(context.getString(pref.prefId), value).commit()

    enum class LongPref(val prefId: Int, val defaultId: Int) {
        last_checked(R.string.pref_last_checked, R.integer.empty_int),
        last_run_manual(R.string.pref_last_manual_run, R.integer.empty_int),
        last_set_manual(R.string.pref_last_manual_set, R.integer.empty_int),
        last_run_automatic(R.string.pref_last_automatic_run, R.integer.empty_int),
        last_set_automatic(R.string.pref_last_automatic_set, R.integer.empty_int),
        last_sync_fix_date(R.string.pref_last_sync_fix_date, R.integer.empty_int)
    }

    fun getLongPref(pref: LongPref) =
        prefs.getLong(context.getString(pref.prefId), context.resources.getInteger(pref.defaultId).toLong())

    fun setLongPref(pref: LongPref, value: Long) = prefs.edit().putLong(context.getString(pref.prefId), value).commit()

    enum class IntPref(val prefId: Int, val defaultId: Int) {
        api_quota(R.string.pref_api_quota, R.integer.empty_int),
        minimum_width(R.string.pref_filtering_width, R.integer.filtering_width_default),
        minimum_height(R.string.pref_filtering_height, R.integer.filtering_height_default),
        filtering_ratio(R.string.pref_filtering_ratio, R.integer.filtering_ratio_default),
        check_time(R.string.pref_automatic_check_time, R.integer.automatic_check_time_default),
        check_variation(R.string.pref_automatic_check_variation, R.integer.automatic_check_variation_default)
    }

    fun getIntPref(pref: IntPref) =
        prefs.getInt(context.getString(pref.prefId), context.resources.getInteger(pref.defaultId).toInt())

    fun setIntPref(pref: IntPref, value: Int) = prefs.edit().putInt(context.getString(pref.prefId), value).commit()
}