package uk.co.jakelee.apodwallpaper.api

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.reactivex.Single
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.config.Config
import uk.co.jakelee.apodwallpaper.helper.*
import java.io.IOException

class ApiWrapper {
    companion object {

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean,
                         postJobTask: () -> Unit): Single<LocalDefinition> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_run_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            var checkedPreviousDay = false
            return Single.fromCallable {
                    val auth = getAuth(prefHelper)
                    try {
                        return@fromCallable ApiClient(Config().getUrl(auth, dateString)).getApodResponse()
                    } catch (e: ApiClient.DateRequestedException) {
                        if (pullingLatest && !checkedPreviousDay) {
                            checkedPreviousDay = true
                            return@fromCallable retryPreviousEntry(dateString, auth)
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
                    val apod = LocalDefinition(it)
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

        private fun getAuth(prefHelper: PreferenceHelper): String {
            var auth = BuildConfig.AUTH_CODE
            if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.custom_key_enabled)
                && prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key).isNotEmpty()
            ) {
                auth = prefHelper.getStringPref(PreferenceHelper.StringPref.custom_key)
            }
            return auth
        }

        private fun retryPreviousEntry(dateString: String, apiKey: String): RemoteDefinition {
            val newDateString = Config().getPreviousEntryDate(dateString)
            return ApiClient(Config().getUrl(apiKey, newDateString)).getApodResponse()
        }

        // If data hasn't been saved before, save it
        // For images, check if image exists. For others, check if title pref set.
        private fun saveDataIfNecessary(
            localDefinition: LocalDefinition,
            fsh: FileSystemHelper,
            prefHelper: PreferenceHelper,
            manualCheck: Boolean
        ) {
            if (localDefinition.isImage && !fsh.getImagePath(localDefinition.date).exists()
                || (!localDefinition.isImage && prefHelper.getApodData(localDefinition.date).title.isEmpty())
            ) {
                prefHelper.saveApodData(localDefinition)
                val lastSetPref =
                    if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                val useHd = prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_hd_images)
                if (localDefinition.isImage) {
                    val image = localDefinition.pullRemoteImage(useHd)
                    fsh.saveImage(image, localDefinition.date)
                }
            }
        }

        private fun handleNewLatestApod(
            localDefinition: LocalDefinition,
            fsh: FileSystemHelper,
            manualCheck: Boolean,
            context: Context,
            prefHelper: PreferenceHelper
        ) {
            if (localDefinition.isImage) {
                val image = fsh.getImage(localDefinition.date)
                if (!manualCheck) {
                    NotificationHelper(context).display(prefHelper, localDefinition, image)
                }
                WallpaperHelper(context, prefHelper).applyRequired(localDefinition.date, image, false)
            }
            prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, localDefinition.date)
        }
    }
}