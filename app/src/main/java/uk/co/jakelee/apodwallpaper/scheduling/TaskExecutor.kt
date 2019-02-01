package uk.co.jakelee.apodwallpaper.scheduling

import android.content.Context
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.firebase.jobdispatcher.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.ApiWrapper.Companion.downloadApod
import uk.co.jakelee.apodwallpaper.api.Apod
import uk.co.jakelee.apodwallpaper.helper.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

// adb shell dumpsys activity service GcmService --endpoints uk.co.jakelee.apodwallpaper
class TaskExecutor : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        // If we're testing scheduling, don't actually perform a job
        if (job.tag == testTag) {
            Toast.makeText(applicationContext, getString(R.string.test_jobs_success), Toast.LENGTH_LONG).show()
            return false
        }
        // If this is the initial task, schedule the regular repeating job
        if (job.tag == initialTaskTag) {
            TaskScheduler(applicationContext).scheduleRepeatingJob()
        }
        downloadApod(
            applicationContext,
            TaskTimingHelper.getLatestDate(),
            true,
            false
        ) { jobFinished(job, false) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
        if (TaskTimingHelper.isJobBadlyTimed(applicationContext)) {
            val tsw = TaskScheduler(applicationContext)
            tsw.cancelJob()
            tsw.scheduleJob()
        }
        return true
    }

    override fun onStopJob(job: JobParameters?) = true

    companion object {
        const val initialTaskTag = "${BuildConfig.APPLICATION_ID}.initialsync"
        const val testTag = "${BuildConfig.APPLICATION_ID}.test"
    }

}