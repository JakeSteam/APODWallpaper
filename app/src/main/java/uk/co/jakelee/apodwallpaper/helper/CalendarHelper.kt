package uk.co.jakelee.apodwallpaper.helper

import java.text.SimpleDateFormat
import java.util.*

class CalendarHelper {
    companion object {
        enum class FORMAT(val value: String) {
            datetime("yyyy-MM-dd HH:mm:ss"),
            date("yyyy-MM-dd"),
            friendlyDate("d MMMM")
        }

        fun millisToString(millis: Long, includeTime: Boolean) = calendarToString(
            Calendar.getInstance().apply { timeInMillis = millis }, includeTime
        )

        fun calendarToString(calendar: Calendar, includeTime: Boolean): String {
            if (calendar.timeInMillis > 0) {
                val dateFormat = if (includeTime) FORMAT.datetime.value else FORMAT.date.value
                return SimpleDateFormat(dateFormat, Locale.US).format(calendar.time)
            }
            return ""
        }

        fun stringToCalendar(dateString: String): Calendar {
            val cal = Calendar.getInstance()
            cal.time = SimpleDateFormat(FORMAT.date.value, Locale.US).parse(dateString)
            return cal
        }

        fun convertFormats(dateString: String, existingFormat: FORMAT, newFormat: FORMAT): String {
            val existingDateFormat = SimpleDateFormat(existingFormat.value, Locale.US)
            val parsedDate = existingDateFormat.parse(dateString)
            return SimpleDateFormat(newFormat.value, Locale.US).format(parsedDate)
        }
    }
}