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
        fun getNextRecheckTime(context: Context) =
            PreferenceHelper(context).getLongPref(PreferenceHelper.LongPref.last_checked) + TimeUnit.MINUTES.toMillis(10)

        fun canRecheck(context: Context) = getNextRecheckTime(context) <= System.currentTimeMillis()

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean): Single<Apod> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_set_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            var checkedPreviousDay = false
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
                    if (!it.isValid()) {
                        throw IOException("Invalid response")
                    }
                    val apod = Apod(it)
                    prefHelper.setIntPref(PreferenceHelper.IntPref.api_quota, it.quota!!)
                    val fsh = FileSystemHelper(context)

                    // If data hasn't been saved before, save it
                    if (!fsh.getImagePath(apod.date).exists()) {
                        prefHelper.saveApodData(apod)
                        val lastSetPref = if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                        prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                        if (apod.isImage) {
                            val image = apod.pullRemoteImage()
                            fsh.saveImage(image, apod.date)
                        }
                    }

                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && apod.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        if (apod.isImage) {
                            val image = fsh.getImage(apod.date)
                            if (!manualCheck) {
                                NotificationHelper(context).display(prefHelper, apod, image)
                            }
                            WallpaperHelper(context, prefHelper).applyRequired(apod.date, image, false)
                        }
                        prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, apod.date)
                    }
                    return@map apod
                }
                .doOnError {
                    if (pullingLatest && it is ApiClient.DateRequestedException && !checkedPreviousDay) {
                        checkedPreviousDay = true
                        val newDateString = CalendarHelper.modifyStringDate(dateString, -1)
                        Timber.i("Trying $newDateString as $dateString was not available")
                        downloadApod(context, newDateString, pullingLatest, manualCheck)
                    } else {
                        Timber.e("Failed to retrieve: ${it.localizedMessage}")
                    }
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