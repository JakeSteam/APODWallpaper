package uk.co.jakelee.apodwallpaper.scheduling

import android.content.Context
import com.firebase.jobdispatcher.*
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import java.util.concurrent.TimeUnit

class EndpointCheckScheduler(val context: Context) {
    fun scheduleJob() {
        val prefsHelper = PreferenceHelper(context)
        if (!prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_enabled)) return
        val timeRemaining = EndpointCheckTimingHelper.getSecondsUntilTarget(prefsHelper)
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val variationSeconds = TimeUnit.MINUTES.toSeconds(variationMinutes.toLong())
        val minTime = if (timeRemaining > variationSeconds) timeRemaining - variationSeconds else 0
        val maxTime = timeRemaining + variationSeconds
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(EndpointCheckJob::class.java)
            .setTag(EndpointCheckJob.INITIAL_JOB_TAG)
            .setRecurring(false)
            .setLifetime(Lifetime.FOREVER)
            .setReplaceCurrent(true)
            .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
            .setTrigger(Trigger.executionWindow(minTime.toInt(), maxTime.toInt()))
            //.setTrigger(Trigger.executionWindow(5, 15))
            .build()
        )
    }

    fun scheduleRepeatingJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val prefsHelper = PreferenceHelper(context)
        val repeatMinutes = TimeUnit.DAYS.toMinutes(1)
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val wifiOnly = prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_check_wifi)
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(EndpointCheckJob::class.java)
            .setTag(EndpointCheckJob.JOB_TAG)
            .setRecurring(true)
            .setLifetime(Lifetime.FOREVER)
            .setReplaceCurrent(true)
            .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
            .setConstraints(if (wifiOnly) Constraint.ON_UNMETERED_NETWORK else 0)
            .setTrigger(
                Trigger.executionWindow(
                    TimeUnit.MINUTES.toSeconds(repeatMinutes - variationMinutes).toInt(),
                    TimeUnit.MINUTES.toSeconds(repeatMinutes + variationMinutes).toInt()
                )
            )
            //.setTrigger(Trigger.executionWindow(5, 15))
            .build()
        )
    }

    fun scheduleTestJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(EndpointCheckJob::class.java)
            .setTag(EndpointCheckJob.TEST_JOB_TAG)
            .setRecurring(false)
            .setTrigger(Trigger.executionWindow(50, 70))
            .build()
        )
    }

    fun cancelJob() = FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
}