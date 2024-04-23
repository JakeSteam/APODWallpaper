package uk.co.jakelee.apodwallpaper.scheduling

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import java.util.concurrent.TimeUnit

class EndpointCheckScheduler(val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleJob() {
        val prefsHelper = PreferenceHelper(context)
        if (!prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_enabled)) return
        val timeRemaining = EndpointCheckTimingHelper.getSecondsUntilTarget(prefsHelper)
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val variationSeconds = TimeUnit.MINUTES.toSeconds(variationMinutes.toLong())
        val minTime = if (timeRemaining > variationSeconds) timeRemaining - variationSeconds else 0

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EndpointCheckJob>()
            .setInitialDelay(minTime, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .addTag(EndpointCheckJob.INITIAL_JOB_TAG)
            .build()

        workManager.enqueueUniqueWork(EndpointCheckJob.INITIAL_JOB_TAG, ExistingWorkPolicy.REPLACE, workRequest)
    }

    fun scheduleRepeatingJob() {
        val prefsHelper = PreferenceHelper(context)
        val repeatMinutes = TimeUnit.DAYS.toMinutes(1)
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val wifiOnly = prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_check_wifi)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<EndpointCheckJob>(repeatMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(EndpointCheckJob.JOB_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(EndpointCheckJob.JOB_TAG, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    fun scheduleTestJob() {
        val workRequest = OneTimeWorkRequestBuilder<EndpointCheckJob>()
            .setInitialDelay(50, TimeUnit.SECONDS)
            .addTag(EndpointCheckJob.TEST_JOB_TAG)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelJob() = workManager.cancelAllWork()
}