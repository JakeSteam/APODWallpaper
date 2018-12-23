package uk.co.jakelee.apodwallpaper.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL

data class Apod(
    val date: String,
    val title: String,
    val desc: String,
    val imageUrl: String,
    val imageUrlHd: String,
    val copyright: String,
    val isImage: Boolean
) {
    constructor(response: ApiResponse) : this(
        response.date,
        response.title,
        response.explanation,
        response.url,
        response.hdurl ?: response.url,
        response.copyright ?: "NASA",
        response.media_type == "image"
    )

    fun pullRemoteImage(useHd: Boolean): Bitmap {
        val url = if (useHd) this.imageUrlHd else this.imageUrl
        return BitmapFactory.decodeStream(URL(url).openStream())
    }
}