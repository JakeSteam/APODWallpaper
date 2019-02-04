package uk.co.jakelee.apodwallpaper.config

import uk.co.jakelee.apodwallpaper.api.ApiResponse

interface IConfig {
    fun getUrl(auth: String, date: String): String

    fun parseResponse(response: String): ApiResponse
}