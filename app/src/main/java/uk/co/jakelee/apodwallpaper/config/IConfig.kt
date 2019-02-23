package uk.co.jakelee.apodwallpaper.config

import android.content.Context
import uk.co.jakelee.apodwallpaper.api.ContentItem

interface IConfig {
    val defaultCopyright: String
    val imageTypeIdentifier: String
    val supportsPaging: Boolean

    fun getUrl(auth: String, date: String): String

    fun parseResponse(context: Context, response: String): ContentItem

    fun getPreviousEntryDate(dateString: String): String
}