package uk.co.jakelee.apodwallpaper.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.MainActivity
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ContentItem


class NotificationHelper(val context: Context) {
    private val channelId = "${BuildConfig.APPLICATION_ID}.channel"
    private val notificationId = 14321

    fun displayLatest() {
        val prefHelper = PreferenceHelper(context)
        val dateString = prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)
        if (!dateString.isEmpty()) {
            val content = ContentHelper(context).getContentData(dateString)
            val image = FileSystemHelper(context).getImage(content.date)
            display(prefHelper, content, image)
        }
    }

    fun display(prefHelper: PreferenceHelper, contentItem: ContentItem, image: Bitmap) {
        if (!prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_enabled)) {
            return
        }
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = applyNotificationPreferences(prefHelper, getBasicNotification(contentItem), image)
        createNotifChannelIfNeeded(notifManager)
        notifManager.notify(notificationId, notification)
    }

    private fun getBasicNotification(contentItem: ContentItem): NotificationCompat.Builder {
        val date = CalendarHelper.convertFormats(
            contentItem.date,
            CalendarHelper.Companion.FORMAT.date,
            CalendarHelper.Companion.FORMAT.friendlyDate
        )
        return NotificationCompat.Builder(context, channelId)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$date: ${contentItem.title}")
            .setContentText(contentItem.desc.take(100))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
    }

    private fun applyNotificationPreferences(
        prefHelper: PreferenceHelper,
        notif: NotificationCompat.Builder,
        image: Bitmap
    ): Notification {
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_led)) {
            notif.setLights(Color.WHITE, 1000, 3000)
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_sound)) {
            notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_vibrate)) {
            notif.setVibrate(longArrayOf(0, 400))
        }
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_preview)) {
            notif.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon(null)
            )
        } else {
            notif.setLargeIcon(image)
        }
        return notif.build()
    }

    private fun createNotifChannelIfNeeded(notifManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }
}