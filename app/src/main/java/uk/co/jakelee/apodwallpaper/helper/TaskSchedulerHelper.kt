package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import com.firebase.jobdispatcher.*
import io.reactivex.Single
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.Apod
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class TaskSchedulerHelper : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        Timber.d("Job started")
        downloadApod(
            applicationContext,
            getLatestDate(),
            true, false)
        return true
    }

    override fun onStopJob(job: JobParameters?) = true


    companion object {
        fun canRecheck(context: Context) =
            System.currentTimeMillis() - PreferenceHelper(context).getLongPref(PreferenceHelper.LongPref.last_checked) > TimeUnit.MINUTES.toMillis(10)

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean): Single<Apod> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_set_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            return Single
                .fromCallable {
                    var apiKey = BuildConfig.APOD_API_KEY
                    if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.custom_key_enabled)) {
                        apiKey = prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key)
                    }
                    val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey&date=$dateString&hd=true"
                    ApiClient(url).getApodResponse()
                }
                .map {
                    if (it.isValid()) {
                        return@map Apod(it)
                    }
                    throw IOException()
                }
                .map {
                    // If data hasn't been saved before, save it
                    val image = it.pullRemoteImage()
                    if (!FileSystemHelper(context).getImagePath(it.date).exists()) {
                        prefHelper.saveApodData(it)
                        FileSystemHelper(context).saveImage(image, it.date)
                        val lastSetPref = if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                        prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                    }
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && it.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, it.date)
                        if (SettingsHelper.setWallpaper) {
                            WallpaperHelper(context).updateWallpaper(image)
                        }
                        if (SettingsHelper.setLockScreen) {
                            WallpaperHelper(context).updateLockScreen(FileSystemHelper(context).getImagePath(it.date))
                        }
                    }
                    return@map it
                }
        }

        fun getLatestDate() = CalendarHelper.calendarToString(Calendar.getInstance(), false)

        fun scheduleJob(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val prefsHelper = PreferenceHelper(context)
            val targetHours = prefsHelper.prefs.getInt(context.getString(R.string.pref_automatic_check_frequency), 24)
            val varianceHours = prefsHelper.prefs.getInt(context.getString(R.string.pref_automatic_check_variance), 5)
            val wifiOnly = prefsHelper.prefs.getBoolean(context.getString(R.string.pref_automatic_check_wifi), false)
            val exampleJob = dispatcher.newJobBuilder()
                .setService(TaskSchedulerHelper::class.java)
                .setTag(BuildConfig.APPLICATION_ID)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(if (wifiOnly) Constraint.ON_UNMETERED_NETWORK else 0)
                .setTrigger(Trigger.executionWindow(
                    TimeUnit.HOURS.toSeconds((targetHours - varianceHours).toLong()).toInt(),
                    TimeUnit.HOURS.toSeconds((targetHours + varianceHours).toLong()).toInt()))
            dispatcher.mustSchedule(exampleJob.build())
            Timber.d("Scheduled job")
        }

        fun cancelJobs(context: Context) {
            FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
        }
    }

}