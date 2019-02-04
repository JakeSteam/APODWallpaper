package uk.co.jakelee.apodwallpaper.config

import android.content.Context
import uk.co.jakelee.apodwallpaper.api.LocalObject

interface IConfig {
    val defaultCopyright: String
    val imageTypeIdentifier: String

    fun getUrl(auth: String, date: String): String

    fun parseResponse(context: Context, response: String): LocalObject

    fun getPreviousEntryDate(dateString: String): String
}