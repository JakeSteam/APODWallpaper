package uk.co.jakelee.apodwallpaper.api

import android.content.Context
import io.reactivex.Single
import uk.co.jakelee.apodwallpaper.config.Config
import uk.co.jakelee.apodwallpaper.helper.*

class ApiWrapper {
    companion object {

        fun downloadContent(context: Context, dateString: String, pullingLatest: Boolean, manualCheck: Boolean): Single<ContentItem> {
            val prefHelper = PreferenceHelper(context)
            val lastRunPref = if (manualCheck) PreferenceHelper.LongPref.last_run_manual else PreferenceHelper.LongPref.last_run_automatic
            prefHelper.setLongPref(lastRunPref, System.currentTimeMillis())
            prefHelper.setLongPref(PreferenceHelper.LongPref.last_checked, System.currentTimeMillis())
            // Latest is asked for directly, rather than guessing today's date and falling back a day,
            // since the device's date is often ahead of the APOD's (US Eastern) one.
            val url = if (pullingLatest) Config().getLatestUrl() else Config().getUrl(dateString)
            return Single.fromCallable { ApiClient(url).getApiResponse(context) }
                .map {
                    val fsh = FileSystemHelper(context)
                    saveDataIfNecessary(it, fsh, prefHelper, ContentHelper(context), manualCheck)
                    // If we're pulling the latest image, and it's different to the current latest
                    if (pullingLatest && it.date != prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)) {
                        handleNewLatestContent(it, fsh, manualCheck, context, prefHelper)
                    }
                    return@map it
                }
        }

        // If data hasn't been saved before, save it
        // For images, check if image exists. For others, check if title pref set.
        private fun saveDataIfNecessary(
            contentItem: ContentItem,
            fsh: FileSystemHelper,
            prefHelper: PreferenceHelper,
            contentHelper: ContentHelper,
            manualCheck: Boolean
        ) {
            if (contentItem.isImage && !fsh.getImagePath(contentItem.date).exists()
                || (!contentItem.isImage && contentHelper.getContentData(contentItem.date).title.isEmpty())
            ) {
                contentHelper.saveContentData(contentItem)
                val lastSetPref =
                    if (manualCheck) PreferenceHelper.LongPref.last_set_manual else PreferenceHelper.LongPref.last_set_automatic
                prefHelper.setLongPref(lastSetPref, System.currentTimeMillis())
                val useHd = prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_hd_images)
                if (contentItem.isImage) {
                    var image = contentItem.pullImageFromServer(useHd)
                    if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.use_greyscale_images)) {
                        image = contentItem.greyscaleImage(image)
                    }
                    fsh.saveImage(image, contentItem.date)
                }
            }
        }

        private fun handleNewLatestContent(
            contentItem: ContentItem,
            fsh: FileSystemHelper,
            manualCheck: Boolean,
            context: Context,
            prefHelper: PreferenceHelper
        ) {
            if (contentItem.isImage) {
                val image = fsh.getImage(contentItem.date)
                if (!manualCheck) {
                    NotificationHelper(context).display(prefHelper, contentItem, image)
                }
                WallpaperHelper(context, prefHelper).applyRequired(contentItem.date, image, false)
            }
            prefHelper.setStringPref(PreferenceHelper.StringPref.last_pulled, contentItem.date)
        }
    }
}