package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import com.firebase.jobdispatcher.*
import io.reactivex.Single
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed
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

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean): Single<ResponseApodProcessed> {
            val prefHelper = PreferenceHelper(context)
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            prefHelper.updateLastRunDate(manualCheck)
            return Single
                .fromCallable {
                    val url = "https://api.nasa.gov/planetary/apod?api_key=${BuildConfig.APOD_API_KEY}&date=$dateString&hd=true"
                    ApiClient(url).getApodResponse()
                }
                .map {
                    val bitmap = it.pullRemoteImage()
                    if (it.isValid()) {
                        return@map ResponseApodProcessed(it, bitmap)
                    }
                    throw IOException()
                }
                .map {
                    // If data hasn't been saved before, save it
                    if (!prefHelper.doesDataExist(context, it.date)) {
                        prefHelper.updateLastSetDate(manualCheck)
                        prefHelper.saveApodData(it)
                        FileSystemHelper(context).saveImage(it.image, it.date)
                    }
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && it.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, it.date)
                        if (SettingsHelper.setWallpaper) {
                            WallpaperHelper(context).updateWallpaper(it.image)
                        }
                        if (SettingsHelper.setLockScreen) {
                            WallpaperHelper(context).updateLockScreen(FileSystemHelper(context).getImage(it.date))
                        }
                    }
                    return@map it
                }
        }

        fun getLatestDate() = CalendarHelper.calendarToString(Calendar.getInstance(), false)

        fun scheduleJob(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val prefsHelper = PreferenceHelper(context)
            val targetHours = prefsHelper.prefs.getInt(context.getString(R.string.automatic_check_frequency), 24)
            val varianceHours = prefsHelper.prefs.getInt(context.getString(R.string.automatic_check_variance), 5)
            val wifiOnly = prefsHelper.prefs.getBoolean(context.getString(R.string.automatic_check_wifi), false)
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