package uk.co.jakelee.apodwallpaper.config

import android.content.Context
import android.net.Uri
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ContentItem
import java.io.IOException
import kotlin.math.roundToInt

// NASA replaced api.nasa.gov/planetary/apod with this endpoint in Sep 2026, and archives the old
// one on 1 Dec 2026. It needs no API key and has no quota.
// https://github.com/nasa/apod-api
class Config {
    private val baseUrl = "https://science.nasa.gov/wp-json/wp/v2/apod-basic"
    private val defaultCopyright = "NASA"
    private val imageTypeIdentifier = "image"
    // Longest edge for non-HD images. The image server upscales too, so smaller images are left alone.
    private val standardMaxEdge = 1920

    // The collection returns the newest entries first, and silently ignores ?date=.
    fun getLatestUrl() = "$baseUrl?per_page=1"

    // yyyy-MM-dd to the endpoint's yyMMdd, as in the old apYYMMDD.html pages.
    fun getUrl(date: String) = "$baseUrl/${date.substring(2).replace("-", "")}"

    fun parseResponse(context: Context, response: String): ContentItem {
        // The collection returns an array, a single date an object.
        val body = response.trim()
        val json = if (body.startsWith("[")) {
            Gson().fromJson(body, Array<RemoteApod>::class.java).firstOrNull()
        } else {
            Gson().fromJson(body, RemoteApod::class.java)
        }
        if (json == null || !json.isValid()) {
            throw IOException(context.getString(R.string.error_returned_apod_format))
        }
        val hdUrl = json.hdurl ?: ""
        return ContentItem(
            json.date!!,
            htmlToText(json.title!!),
            cleanExplanation(json.explanation ?: ""),
            getStandardImageUrl(hdUrl),
            hdUrl,
            cleanCredit(json.copyright ?: json.credit ?: ""),
            json.media_type == imageTypeIdentifier && hdUrl.isNotEmpty(),
            json.permalink ?: ""
        )
    }

    // hdurl carries the original size as ?w=&h=; without those the server returns a 1280px copy.
    private fun getStandardImageUrl(hdUrl: String): String {
        val uri = Uri.parse(hdUrl)
        val width = uri.getQueryParameter("w")?.toIntOrNull() ?: return hdUrl
        val height = uri.getQueryParameter("h")?.toIntOrNull() ?: return hdUrl
        val scale = standardMaxEdge.toDouble() / maxOf(width, height)
        if (scale >= 1) {
            return hdUrl
        }
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames.forEach { name ->
            val value = when (name) {
                "w" -> (width * scale).roundToInt().toString()
                "h" -> (height * scale).roundToInt().toString()
                else -> uri.getQueryParameter(name)
            }
            builder.appendQueryParameter(name, value)
        }
        return builder.build().toString()
    }

    // Starts with a bold "Explanation:" label, and ends with a teaser for the next day's APOD.
    private fun cleanExplanation(html: String) = htmlToText(html)
        .replace(Regex("^Explanation:\\s*"), "")
        .replace(Regex("\\s*Tomorrow's picture:.*$", RegexOption.DOT_MATCHES_ALL), "")

    // Older entries prefix a label such as "Image Credit & Copyright:".
    private fun cleanCredit(html: String) = htmlToText(html)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("^[^:]{0,40}Credit[^:]*:\\s*", RegexOption.IGNORE_CASE), "")
        .ifEmpty { defaultCopyright }

    private fun htmlToText(html: String) = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(' ', ' ')
        .trim()
}
