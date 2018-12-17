package uk.co.jakelee.apodwallpaper.helper

import java.text.SimpleDateFormat
import java.util.*

class CalendarHelper {
    companion object {
        fun millisToString(millis: Long, includeTime: Boolean) = calendarToString(
                Calendar.getInstance().apply { timeInMillis = millis}, includeTime
            )

        fun calendarToString(calendar: Calendar, includeTime: Boolean): String {
            if (calendar.timeInMillis > 0) {
                val dateFormat = if (includeTime) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd"
                return SimpleDateFormat(dateFormat, Locale.US).format(calendar.time)
            }
            return ""
        }

        fun stringToCalendar(dateString: String): Calendar {
            val cal = Calendar.getInstance()
            cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
            return cal
        }

        fun modifyStringDate(dateString: String, days: Int): String {
            val date = CalendarHelper.stringToCalendar(dateString)
            date.add(Calendar.DAY_OF_YEAR, days)
            return CalendarHelper.calendarToString(date, false)
        }
    }
}