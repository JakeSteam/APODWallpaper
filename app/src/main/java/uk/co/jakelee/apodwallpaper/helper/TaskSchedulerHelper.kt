package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.Single
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
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
            true
        )
        return true
    }

    override fun onStopJob(job: JobParameters?) = true


    companion object {
        fun canRecheck(context: Context) =
            System.currentTimeMillis() - PreferenceHelper(context).getLastCheckedDate() > TimeUnit.MINUTES.toMillis(10)

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean): Single<ResponseApodProcessed> {
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
                    val prefHelper = PreferenceHelper(context)
                    // If data hasn't been saved before, save it
                    if (!prefHelper.doesDataExist(it.date)) {
                        prefHelper.saveApodData(it)
                        FileSystemHelper(context).saveImage(it.image, it.date)
                    }
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && it.date != prefHelper.getLastPulledDate()) {
                        prefHelper.updateLastPulledDate(it.date)
                        prefHelper.updateLastCheckedDate()
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

        fun getLatestDate() = CalendarHelper.calendarToString(Calendar.getInstance())

        fun scheduleJob(context: Context) {
            /*val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val exampleJob = dispatcher.newJobBuilder()
                .setService(JobScheduler::class.java)
                .setTag("job tag")
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setTrigger(Trigger.executionWindow(5, 10))
            dispatcher.mustSchedule(exampleJob.build())
            Timber.d("Scheduled job")*/
        }

        fun cancelJobs(context: Context) {
            FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
        }
    }

}