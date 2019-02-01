package uk.co.jakelee.apodwallpaper.api

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.reactivex.Single
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.helper.*
import uk.co.jakelee.apodwallpaper.scheduling.TaskExecutor
import java.io.IOException

class ApiWrapper {
    companion object {
        fun getUrl(apiKey: String, date: String) =
            "https://api.nasa.gov/planetary/apod?api_key=$apiKey&date=$date&hd=true"

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean,
                         postJobTask: () -> Unit): Single<Apod> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_run_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            var checkedPreviousDay = false
            return Single.fromCallable {
                    val apiKey = getApiKey(prefHelper)
                    try {
                        return@fromCallable ApiClient(ApiWrapper.getUrl(apiKey, dateString)).getApodResponse()
                    } catch (e: ApiClient.DateRequestedException) {
                        if (pullingLatest && !checkedPreviousDay) {
                            checkedPreviousDay = true
                            return@fromCallable retryPreviousDay(dateString, apiKey)
                        } else {
                            throw ApiClient.DateRequestedException()
                        }
                    }
                }
                .map {
                    if (!it.isValid()) {
                        throw IOException(context.getString(R.string.error_returned_apod_format))
                    }
                    val fsh = FileSystemHelper(context)
                    val apod = Apod(it)
                    prefHelper.setIntPref(PreferenceHelper.IntPref.api_quota, it.quota!!)
                    saveDataIfNecessary(apod, fsh, prefHelper, manualCheck)
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && apod.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        handleNewLatestApod(apod, fsh, manualCheck, context, prefHelper)
                    }
                    postJobTask.invoke()
                    return@map apod
                }
                .doOnError {
                    Crashlytics.setBool("forced log", true)
                    Crashlytics.logException(it)
                }
        }

        private fun getApiKey(prefHelper: PreferenceHelper): String {
            var apiKey = BuildConfig.APOD_API_KEY
            if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.custom_key_enabled)
                && prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key).isNotEmpty()
            ) {
                apiKey = prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key)
            }
            return apiKey
        }

        private fun retryPreviousDay(dateString: String, apiKey: String): ApiResponse {
            val newDateString = CalendarHelper.modifyStringDate(dateString, -1)
            return ApiClient(getUrl(apiKey, newDateString)).getApodResponse()
        }

        // If data hasn't been saved before, save it
        // For images, check if image exists. For others, check if title pref set.
        fun saveDataIfNecessary(
            apod: Apod,
            fsh: FileSystemHelper,
            prefHelper: PreferenceHelper,
            manualCheck: Boolean
        ) {
            if (apod.isImage && !fsh.getImagePath(apod.date).exists()
                || (!apod.isImage && prefHelper.getApodData(apod.date).title.isEmpty())
            ) {
                prefHelper.saveApodData(apod)
                val lastSetPref =
                    if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                val useHd = prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_hd_images)
                if (apod.isImage) {
                    val image = apod.pullRemoteImage(useHd)
                    fsh.saveImage(image, apod.date)
                }
            }
        }

        fun handleNewLatestApod(
            apod: Apod,
            fsh: FileSystemHelper,
            manualCheck: Boolean,
            context: Context,
            prefHelper: PreferenceHelper
        ) {
            if (apod.isImage) {
                val image = fsh.getImage(apod.date)
                if (!manualCheck) {
                    NotificationHelper(context).display(prefHelper, apod, image)
                }
                WallpaperHelper(context, prefHelper).applyRequired(apod.date, image, false)
            }
            prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, apod.date)
        }
    }
}