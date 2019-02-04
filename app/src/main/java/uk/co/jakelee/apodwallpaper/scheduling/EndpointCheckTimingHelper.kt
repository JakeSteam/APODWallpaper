package uk.co.jakelee.apodwallpaper.scheduling

import android.content.Context
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import java.util.*
import java.util.concurrent.TimeUnit

class EndpointCheckTimingHelper {
    companion object {
        fun getSecondsUntilTarget(prefsHelper: PreferenceHelper): Long {
            val targetHour = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_time)
            val currentTime = Calendar.getInstance()
            val targetTime = (currentTime.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (targetTime < currentTime) {
                targetTime.add(Calendar.DAY_OF_YEAR, 1)
            }
            val timeRemaining = targetTime.timeInMillis - currentTime.timeInMillis
            return TimeUnit.MILLISECONDS.toSeconds(timeRemaining)
        }

        fun getLatestDate() =
            CalendarHelper.calendarToString(Calendar.getInstance(), false)

        fun isJobBadlyTimed(context: Context): Boolean {
            val prefHelper = PreferenceHelper(context)
            return prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_check_fix) && isJobDelayed(prefHelper)
        }

        private fun isJobDelayed(prefHelper: PreferenceHelper): Boolean {
            val targetHour = prefHelper.getIntPref(PreferenceHelper.IntPref.check_time)
            val variance = prefHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
            val currentTime = Calendar.getInstance()
            val min = (currentTime.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -variance)
            }
            val max = (currentTime.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, variance)
            }
            return currentTime < min || currentTime > max
        }

        fun getNextRecheckTime(context: Context) =
            PreferenceHelper(context).getLongPref(PreferenceHelper.LongPref.last_checked) + TimeUnit.MINUTES.toMillis(10)

        fun canRecheck(context: Context) = getNextRecheckTime(context) <= System.currentTimeMillis()
    }
}