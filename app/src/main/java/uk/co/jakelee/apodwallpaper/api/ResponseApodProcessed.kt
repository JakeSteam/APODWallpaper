package uk.co.jakelee.apodwallpaper.api

import android.graphics.Bitmap

data class ProcessedApodResponse(
    val title: String,
    val desc: String,
    val date: String,
    val imageUrl: String,
    val image: Bitmap
) {
    constructor(response: ApodResponse, image: Bitmap) : this(
        response.title,
        response.explanation,
        response.date,
        response.hdurl ?: response.url,
        image
    )
}