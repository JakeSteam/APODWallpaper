package uk.co.jakelee.apodwallpaper.config

import android.content.Context
import com.google.gson.Gson
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ContentItem
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import java.io.IOException
import java.util.*

class Config: IConfig {
    override val defaultCopyright = "NASA"
    override val imageTypeIdentifier = "image"
    override val supportsPaging = true

    override fun getPreviousEntryDate(dateString: String): String {
        val date = CalendarHelper.stringToCalendar(dateString)
        date.add(Calendar.DAY_OF_YEAR, -1)
        return CalendarHelper.calendarToString(date, false)
    }

    override fun getUrl(auth: String, date: String) =
        "https://api.nasa.gov/planetary/apod?api_key=$auth&date=$date&hd=true"

    override fun parseResponse(context: Context, response: String): ContentItem {
        val remoteObject = Gson().fromJson(response, RemoteApod::class.java)!!
        if (!remoteObject.isValid()) {
            throw IOException(context.getString(R.string.error_returned_apod_format))
        } else {
            return ContentItem(
                remoteObject.date,
                remoteObject.title,
                remoteObject.explanation,
                remoteObject.url,
                remoteObject.hdurl ?: remoteObject.url,
                remoteObject.copyright ?: Config().defaultCopyright,
                remoteObject.media_type == Config().imageTypeIdentifier
            )
        }
    }
}