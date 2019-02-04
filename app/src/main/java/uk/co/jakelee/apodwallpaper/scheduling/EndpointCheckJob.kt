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
        if (job.tag == TEST_JOB_TAG) {
            Toast.makeText(applicationContext, getString(R.string.test_jobs_success), Toast.LENGTH_LONG).show()
            return false
        }
        // If this is the initial task, also schedule the regular repeating job
        if (job.tag == INITIAL_JOB_TAG) {
            EndpointCheckScheduler(applicationContext).scheduleRepeatingJob()
        }
        val date = EndpointCheckTimingHelper.getLatestDate()
        downloadApod(applicationContext, date, true, false) { jobFinished(job, false) }
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
        const val INITIAL_JOB_TAG = "${BuildConfig.APPLICATION_ID}.initialsync"
        const val JOB_TAG = BuildConfig.APPLICATION_ID
        const val TEST_JOB_TAG = "${BuildConfig.APPLICATION_ID}.test"
    }

}