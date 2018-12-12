package uk.co.jakelee.apodwallpaper

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

fun getResponse(url: String): ApodResponse {
    val request = Request.Builder()
        .url(url)
        .addHeader("Content-Type", "application/json")
        .build()
    val response = OkHttpClient().newCall(request).execute()
    if (response.isSuccessful) {
        response.body()?.string()?.let {
            return Gson().fromJson(it, ApodResponse::class.java)
        }
        throw IOException()
    } else {
        throw IOException()
    }
}

data class ApodResponse(
    val copyright: String,
    val date: String,
    val explanation: String,
    val hdurl: String,
    val media_type: String,
    val service_version: String,
    val title: String,
    val url: String
)