package uk.co.jakelee.apodwallpaper.helper

import java.text.SimpleDateFormat
import java.util.*

class CalendarHelper {
    companion object {
        fun stringToTrio(dateString: String): Triple<Int, Int, Int> {
            val split = dateString.split("-").map { it.toInt() }
            return Triple(split[0], split[1], split[2])
        }

        fun calendarToString(calendar: Calendar) =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        fun stringToCalendar(dateString: String): Calendar {
            val cal = Calendar.getInstance()
            cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
            return cal
        }

        fun modifyStringDate(dateString: String, days: Int): String {
            val date = CalendarHelper.stringToCalendar(dateString)
            date.add(Calendar.DAY_OF_YEAR, days)
            return CalendarHelper.calendarToString(date)
        }
    }
}