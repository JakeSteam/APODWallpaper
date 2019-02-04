package uk.co.jakelee.apodwallpaper.config

interface IConfig {
    val defaultCopyright: String
    val imageTypeIdentifier: String

    fun getUrl(auth: String, date: String): String

    fun parseResponse(response: String): ApodResponse

    fun getPreviousEntryDate(dateString: String): String
}