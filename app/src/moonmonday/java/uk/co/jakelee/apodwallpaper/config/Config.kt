package uk.co.jakelee.apodwallpaper.config

import android.content.Context
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ContentItem
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

class Config: IConfig {
    override val defaultCopyright = "NASA"
    override val imageTypeIdentifier = ""
    override val supportsPaging = false

    override fun getPreviousEntryDate(dateString: String) = ""

    override fun getUrl(auth: String, date: String) = "https://moonmonday.space/feed/"

    override fun parseResponse(context: Context, response: String): ContentItem {
        try {
            return responseToContentItem(response)
        } catch (e: Exception) {
            throw IOException(context.getString(R.string.error_returned_apod_format))
        }
    }


    private fun responseToContentItem(response: String): ContentItem {
        val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(response)))
        xmlDoc.documentElement.normalize()
        val items = xmlDoc.getElementsByTagName("item")
        val latestItem = items.item(0)
        val latestElem = latestItem as Element
        val apiDateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        val postDate = SimpleDateFormat(apiDateFormat, Locale.US).parse(latestElem.getElementsByTagName("pubDate").item(0).firstChild.textContent)
        return ContentItem(
            SimpleDateFormat(CalendarHelper.Companion.FORMAT.date.value, Locale.US).format(postDate),
            latestElem.getElementsByTagName("title").item(0).firstChild.textContent,
            latestElem.getElementsByTagName("description").item(0).firstChild.textContent,
            (latestElem.getElementsByTagName("media:content").item(1) as Element).getAttribute("url"),
            (latestElem.getElementsByTagName("media:content").item(1) as Element).getAttribute("url"),
            Config().defaultCopyright,
            true
        )
    }
}