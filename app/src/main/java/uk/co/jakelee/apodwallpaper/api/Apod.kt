package uk.co.jakelee.apodwallpaper.api

import android.graphics.BitmapFactory
import java.net.URL

data class Apod(
    val date: String,
    val title: String,
    val desc: String,
    val imageUrl: String,
    val imageUrlHd: String,
    val copyright: String
) {
    constructor(response: ApiResponse) : this(
        response.date,
        response.title,
        response.explanation,
        response.url,
        response.hdurl ?: response.url,
        response.copyright ?: "NASA"
    )

    fun pullRemoteImage() = BitmapFactory.decodeStream(URL(this.imageUrl).openStream())
}