package uk.co.jakelee.apodwallpaper.config

import com.google.gson.Gson
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import java.util.*

class Config: IConfig {
    override val defaultCopyright = "NASA"
    override val imageTypeIdentifier = "image"

    override fun getPreviousEntryDate(dateString: String): String {
        val date = CalendarHelper.stringToCalendar(dateString)
        date.add(Calendar.DAY_OF_YEAR, -1)
        return CalendarHelper.calendarToString(date, false)
    }

    override fun getUrl(auth: String, date: String) =
        "https://api.nasa.gov/planetary/apod?api_key=$auth&date=$date&hd=true"

    override fun parseResponse(response: String) = Gson().fromJson(response, ApodResponse::class.java)!!
}