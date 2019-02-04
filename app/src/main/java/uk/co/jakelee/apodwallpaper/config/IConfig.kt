package uk.co.jakelee.apodwallpaper.config

import uk.co.jakelee.apodwallpaper.api.RemoteDefinition

interface IConfig {
    val defaultCopyright: String
    val imageTypeIdentifier: String

    fun getUrl(auth: String, date: String): String

    fun parseResponse(response: String): RemoteDefinition

    fun getPreviousEntryDate(dateString: String): String
}