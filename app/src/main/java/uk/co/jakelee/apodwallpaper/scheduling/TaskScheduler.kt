package uk.co.jakelee.apodwallpaper.scheduling

import android.content.Context
import com.firebase.jobdispatcher.*
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import java.util.concurrent.TimeUnit

class TaskScheduler(val context: Context) {
    fun scheduleJob() {
        val prefsHelper = PreferenceHelper(context)
        if (!prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_enabled)) return
        val timeRemaining = TaskTimingHelper.getSecondsUntilTarget(prefsHelper)
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val variationSeconds = TimeUnit.MINUTES.toSeconds(variationMinutes.toLong())
        val minTime = if (timeRemaining > variationSeconds) timeRemaining - variationSeconds else 0
        val maxTime = timeRemaining + variationSeconds
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(TaskExecutor::class.java)
            .setTag(TaskExecutor.initialTaskTag)
            .setRecurring(false)
            .setLifetime(Lifetime.FOREVER)
            .setReplaceCurrent(true)
            .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
            .setTrigger(Trigger.executionWindow(minTime.toInt(), maxTime.toInt()))
            //.setTrigger(Trigger.executionWindow(5, 15))
            .build()
        )
        Timber.d("Scheduled repeating job")
    }

    fun scheduleRepeatingJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val prefsHelper = PreferenceHelper(context)
        val repeatMinutes = TimeUnit.DAYS.toMinutes(1)
        val variationMinutes = prefsHelper.getIntPref(PreferenceHelper.IntPref.check_variation)
        val wifiOnly = prefsHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_check_wifi)
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(TaskExecutor::class.java)
            .setTag(BuildConfig.APPLICATION_ID)
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
        Timber.d("Scheduled repeating job")
    }

    fun scheduleTestJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
            .setService(TaskExecutor::class.java)
            .setTag(TaskExecutor.testTag)
            .setRecurring(false)
            .setTrigger(Trigger.executionWindow(50, 70))
            .build()
        )
    }

    fun cancelJob() = FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
}