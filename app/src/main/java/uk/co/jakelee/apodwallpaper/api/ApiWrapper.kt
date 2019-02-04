package uk.co.jakelee.apodwallpaper.api

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.reactivex.Single
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.config.Config
import uk.co.jakelee.apodwallpaper.config.RemoteObject
import uk.co.jakelee.apodwallpaper.helper.*
import java.io.IOException

class ApiWrapper {
    companion object {

        fun downloadApod(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean,
                         postJobTask: () -> Unit): Single<LocalObject> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_run_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            var checkedPreviousDay = false
            return Single.fromCallable {
                    val auth = getAuth(prefHelper)
                    try {
                        return@fromCallable ApiClient(Config().getUrl(auth, dateString)).getApodResponse(context)
                    } catch (e: ApiClient.DateRequestedException) {
                        if (pullingLatest && !checkedPreviousDay) {
                            checkedPreviousDay = true
                            return@fromCallable retryPreviousEntry(context, dateString, auth)
                        } else {
                            throw ApiClient.DateRequestedException()
                        }
                    }
                }
                .map {
                    val fsh = FileSystemHelper(context)
                    prefHelper.setIntPref(PreferenceHelper.IntPref.api_quota, it.second)
                    saveDataIfNecessary(it.first, fsh, prefHelper, manualCheck)
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && it.first.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        handleNewLatestApod(it.first, fsh, manualCheck, context, prefHelper)
                    }
                    postJobTask.invoke()
                    return@map it.first
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

        private fun retryPreviousEntry(context: Context, dateString: String, apiKey: String): Pair<LocalObject, Int> {
            val newDateString = Config().getPreviousEntryDate(dateString)
            return ApiClient(Config().getUrl(apiKey, newDateString)).getApodResponse(context)
        }

        // If data hasn't been saved before, save it
        // For images, check if image exists. For others, check if title pref set.
        private fun saveDataIfNecessary(
            localObject: LocalObject,
            fsh: FileSystemHelper,
            prefHelper: PreferenceHelper,
            manualCheck: Boolean
        ) {
            if (localObject.isImage && !fsh.getImagePath(localObject.date).exists()
                || (!localObject.isImage && prefHelper.getApodData(localObject.date).title.isEmpty())
            ) {
                prefHelper.saveApodData(localObject)
                val lastSetPref =
                    if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                val useHd = prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_hd_images)
                if (localObject.isImage) {
                    val image = localObject.pullRemoteImage(useHd)
                    fsh.saveImage(image, localObject.date)
                }
            }
        }

        private fun handleNewLatestApod(
            localObject: LocalObject,
            fsh: FileSystemHelper,
            manualCheck: Boolean,
            context: Context,
            prefHelper: PreferenceHelper
        ) {
            if (localObject.isImage) {
                val image = fsh.getImage(localObject.date)
                if (!manualCheck) {
                    NotificationHelper(context).display(prefHelper, localObject, image)
                }
                WallpaperHelper(context, prefHelper).applyRequired(localObject.date, image, false)
            }
            prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, localObject.date)
        }
    }
}