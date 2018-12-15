package uk.co.jakelee.apodwallpaper.api

import android.graphics.Bitmap

data class ResponseApodProcessed(
    val date: String,
    val title: String,
    val desc: String,
    val imageUrl: String,
    val imageUrlHd: String,
    val copyright: String,
    val image: Bitmap
) {
    constructor(response: ResponseApod, image: Bitmap) : this(
        response.date,
        response.title,
        response.explanation,
        response.url,
        response.hdurl ?: response.url,
        response.copyright ?: "NASA",
        image
    )
}