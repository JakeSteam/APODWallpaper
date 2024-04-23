package uk.co.jakelee.apodwallpaper.scheduling

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.api.ApiWrapper.Companion.downloadContent

// adb shell dumpsys activity service GcmService --endpoints uk.co.jakelee.apodwallpaper
class EndpointCheckJob(context: Context, params: WorkerParameters) : RxWorker(context, params) {
    init {
        RxJavaPlugins.setErrorHandler { Timber.e(it.cause, "Uncaught RxJava error") }
    }

    @SuppressLint("CheckResult")
    override fun createWork(): Single<Result> {
        // If we're testing scheduling, don't actually perform a job
        if (id.toString() == TEST_JOB_TAG) {
            return Single.just(Result.success())
        }
        // If this is the initial task, also schedule the regular repeating job
        if (id.toString() == INITIAL_JOB_TAG) {
            EndpointCheckScheduler(applicationContext).scheduleRepeatingJob()
        }
        val date = EndpointCheckTimingHelper.getLatestDate()
        return downloadContent(applicationContext, date, true, false)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { Result.success() } // Map ContentItem to Result.success()
            .doOnError {
                Timber.e("Exception during job, try again")
            }
            .onErrorReturnItem(Result.retry())
    }

    companion object {
        const val INITIAL_JOB_TAG = "${BuildConfig.APPLICATION_ID}.initialsync"
        const val JOB_TAG = BuildConfig.APPLICATION_ID
        const val TEST_JOB_TAG = "${BuildConfig.APPLICATION_ID}.test"
    }
}