package uk.co.jakelee.apodwallpaper

import android.content.Context
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.Single
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.ResponseApodProcessed
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.helper.SettingsHelper
import uk.co.jakelee.apodwallpaper.helper.WallpaperHelper
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class JobScheduler : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        Timber.d("Job started")
        downloadApod(applicationContext, getLatestDate())
        return true
    }

    override fun onStopJob(job: JobParameters?) = true


    companion object {
        fun downloadApod(context: Context, dateString: String): Single<ResponseApodProcessed> {
            val url = "https://api.nasa.gov/planetary/apod?api_key=${BuildConfig.APOD_API_KEY}&date=$dateString&hd=true"
            return Single
                .fromCallable { ApiClient(url).getApodResponse() }
                .map {
                    val bitmap = it.pullRemoteImage()
                    if (it.isValid()) {
                        return@map ResponseApodProcessed(it, bitmap)
                    }
                    throw IOException()
                }
                .map {
                    val prefHelper = PreferenceHelper(context)
                    prefHelper.updateLastCheckedDate()
                    if (prefHelper.getLastPulledDate() != it.date) {
                        prefHelper.updateLastPulledDate(it.date)
                        prefHelper.saveApodData(it)
                        FileSystemHelper(context).saveImage(it.image, it.date)
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

        fun getLatestDate() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

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