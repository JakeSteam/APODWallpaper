package uk.co.jakelee.apodwallpaper.api

import android.graphics.BitmapFactory
import java.net.URL

data class ResponseApod(
    val copyright: String,
    val date: String,
    val explanation: String,
    val hdurl: String?,
    val media_type: String,
    val service_version: String,
    val title: String,
    val url: String
) {
    fun pullRemoteImage() = BitmapFactory.decodeStream(URL(this.url).openStream())

    fun isValid() =
        this.media_type == "image" && this.title.isNotEmpty() && (!this.hdurl.isNullOrEmpty() || !this.url.isEmpty())
}