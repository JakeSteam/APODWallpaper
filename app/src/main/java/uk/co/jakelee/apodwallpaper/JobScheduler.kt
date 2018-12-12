package uk.co.jakelee.apodwallpaper

import android.content.Context
import android.util.Log
import com.firebase.jobdispatcher.*

class JobScheduler : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        Log.d("JobScheduler", "Job started")
        when (job.tag) {
            SIMPLE_JOB_TAG -> simpleJob(job)
            else -> return false
        }
        return true
    }

    override fun onStopJob(job: JobParameters?) = true

    private fun simpleJob(job: JobParameters) {
        Log.d("JobScheduler", "Ran job ${job.tag}")
        jobFinished(job, false)
    }

    companion object {
        private const val SIMPLE_JOB_TAG = "uk.co.jakelee.scheduledjobs.job"

        fun scheduleJob(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val exampleJob = dispatcher.newJobBuilder()
                .setService(JobScheduler::class.java)
                .setTag(SIMPLE_JOB_TAG)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setTrigger(Trigger.executionWindow(5, 10))
            dispatcher.mustSchedule(exampleJob.build())
            Log.d("JobScheduler", "Scheduled job")
        }

        fun cancelJobs(context: Context) {
            FirebaseJobDispatcher(GooglePlayDriver(context)).cancelAll()
        }
    }

}