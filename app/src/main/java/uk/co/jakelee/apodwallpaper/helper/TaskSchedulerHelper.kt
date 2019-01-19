package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.firebase.jobdispatcher.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.Apod
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

// adb shell dumpsys activity service GcmService --endpoints uk.co.jakelee.apodwallpaper
class TaskSchedulerHelper : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        Timber.d("Job started")
        downloadApod(applicationContext, getLatestDate(), true, false) { jobFinished(job, false) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
        return true
    }

    override fun onStopJob(job: JobParameters?) = true

    companion object {
        fun getNextRecheckTime(context: Context) =
            PreferenceHelper(context).getLongPref(PreferenceHelper.LongPref.last_checked) + TimeUnit.MINUTES.toMillis(10)

        fun canRecheck(context: Context) = getNextRecheckTime(context) <= System.currentTimeMillis()

        fun getUrl(apiKey: String, date: String) =
            "https://api.nasa.gov/planetary/apod?api_key=$apiKey&date=$date&hd=true"

        fun downloadApod(
            context: Context,
            dateString: String,
            pullingLatest: Boolean,
            manualCheck: Boolean,
            postJobTask: () -> Unit
        ): Single<Apod> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref =
                if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_run_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            var checkedPreviousDay = false
            return Single
                .fromCallable {
                    var apiKey = BuildConfig.APOD_API_KEY
                    if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.custom_key_enabled)) {
                        apiKey = prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key)
                    }
                    try {
                        return@fromCallable ApiClient(getUrl(apiKey, dateString)).getApodResponse()
                    } catch (e: ApiClient.DateRequestedException) {
                        // Retry previous day, due to time differences / delay in releases!
                        if (pullingLatest && !checkedPreviousDay) {
                            checkedPreviousDay = true
                            val newDateString = CalendarHelper.modifyStringDate(dateString, -1)
                            Timber.i("Trying $newDateString as $dateString was not available")
                            return@fromCallable ApiClient(getUrl(apiKey, newDateString)).getApodResponse()
                        } else {
                            throw ApiClient.DateRequestedException()
                        }
                    }
                }
                .map {
                    if (!it.isValid()) {
                        throw IOException(context.getString(R.string.error_returned_apod_format))
                    }
                    val fsh = FileSystemHelper(context)
                    val apod = Apod(it)
                    prefHelper.setIntPref(PreferenceHelper.IntPref.api_quota, it.quota!!)

                    saveDataIfNecessary(apod, fsh, prefHelper, manualCheck)
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && apod.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        handleNewLatestApod(apod, fsh, manualCheck, context, prefHelper)
                    }
                    postJobTask.invoke()
                    return@map apod
                }
                .doOnError {
                    Crashlytics.logException(it)
                }
        }

        // If data hasn't been saved before, save it
        // For images, check if image exists. For others, check if title pref set.
        private fun saveDataIfNecessary(
            apod: Apod,
            fsh: FileSystemHelper,
            prefHelper: PreferenceHelper,
            manualCheck: Boolean
        ) {
            if (apod.isImage && !fsh.getImagePath(apod.date).exists()
                || (!apod.isImage && prefHelper.getApodData(apod.date).title.isEmpty())
            ) {
                prefHelper.saveApodData(apod)
                val lastSetPref =
                    if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                val useHd = prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_hd_images)
                if (apod.isImage) {
                    val image = apod.pullRemoteImage(useHd)
                    fsh.saveImage(image, apod.date)
                }
            }
        }

        private fun handleNewLatestApod(
            apod: Apod,
            fsh: FileSystemHelper,
            manualCheck: Boolean,
            context: Context,
            prefHelper: PreferenceHelper
        ) {
            if (apod.isImage) {
                val image = fsh.getImage(apod.date)
                if (!manualCheck) {
                    NotificationHelper(context).display(prefHelper, apod, image)
                }
                WallpaperHelper(context, prefHelper).applyRequired(apod.date, image, false)
            }
            prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, apod.date)
        }

        fun getLatestDate() = CalendarHelper.calendarToString(Calendar.getInstance(), false)

        fun scheduleJob(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val prefsHelper = PreferenceHelper(context)
            val targetHours = prefsHelper.prefs.getInt(
                context.getString(R.string.pref_automatic_check_frequency),
                context.resources.getInteger(R.integer.automatic_check_frequency_default)
            )
            val varianceHours = prefsHelper.prefs.getInt(
                context.getString(R.string.pref_automatic_check_variance),
                context.resources.getInteger(R.integer.automatic_check_variance_default)
            )
            val wifiOnly = prefsHelper.prefs.getBoolean(context.getString(R.string.pref_automatic_check_wifi), false)
            val exampleJob = dispatcher.newJobBuilder()
                .setService(TaskSchedulerHelper::class.java)
                .setTag(BuildConfig.APPLICATION_ID)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(if (wifiOnly) Constraint.ON_UNMETERED_NETWORK else 0)
                .setTrigger(
                    Trigger.executionWindow(
                        TimeUnit.HOURS.toSeconds((targetHours - varianceHours).toLong()).toInt(),
                        TimeUnit.HOURS.toSeconds((targetHours + varianceHours).toLong()).toInt()
                    )
                )
            //.setTrigger(Trigger.executionWindow(5, 15))
            dispatcher.mustSchedule(exampleJob.build())
            Timber.d("Scheduled job")
        }

        fun scheduleRepeatingJob(context: Context) {
            // this gets called instead, scheduled for the TARGET time
            // when it runs, it does the sync AND schedules the above job for 24hr (+/- variance)
        }

        fun cancelJob(context: Context) = FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
    }

}