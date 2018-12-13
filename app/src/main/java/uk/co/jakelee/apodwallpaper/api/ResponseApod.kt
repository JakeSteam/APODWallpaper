package uk.co.jakelee.apodwallpaper.api

import android.graphics.BitmapFactory
import java.net.URL

data class ApodResponse(
    val copyright: String,
    val date: String,
    val explanation: String,
    val hdurl: String?,
    val media_type: String,
    val service_version: String,
    val title: String,
    val url: String
) {
    fun pullRemoteImage() = BitmapFactory.decodeStream(URL(this.hdurl ?: this.url).openStream())
}