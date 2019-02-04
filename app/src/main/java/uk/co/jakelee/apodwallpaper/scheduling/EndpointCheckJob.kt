package uk.co.jakelee.apodwallpaper.scheduling

import android.widget.Toast
import com.firebase.jobdispatcher.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiWrapper.Companion.downloadApod

// adb shell dumpsys activity service GcmService --endpoints uk.co.jakelee.apodwallpaper
class EndpointCheckJob : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        // If we're testing scheduling, don't actually perform a job
        if (job.tag == testTag) {
            Toast.makeText(applicationContext, getString(R.string.test_jobs_success), Toast.LENGTH_LONG).show()
            return false
        }
        // If this is the initial task, schedule the regular repeating job
        if (job.tag == initialTaskTag) {
            EndpointCheckScheduler(applicationContext).scheduleRepeatingJob()
        }
        downloadApod(applicationContext, EndpointCheckTimingHelper.getLatestDate(), true, false) {
            jobFinished(job, false)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
        if (EndpointCheckTimingHelper.isJobBadlyTimed(applicationContext)) {
            val tsw = EndpointCheckScheduler(applicationContext)
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