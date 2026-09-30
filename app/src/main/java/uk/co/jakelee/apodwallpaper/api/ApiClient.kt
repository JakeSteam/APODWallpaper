package uk.co.jakelee.apodwallpaper.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.co.jakelee.apodwallpaper.config.Config
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ApiClient(val url: String) {

    fun getApiResponse(context: Context): ContentItem {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response.body()?.string()?.let {
                return Config().parseResponse(context, it)
            }
            throw IOException()
        } else {
            when (response.code()) {
                404 -> throw NoApodForDateException()
                500 -> throw ServerError()
                503, 504 -> throw TimeoutException()
                else -> throw UnknownError()
            }
        }
    }

    class NoApodForDateException : Exception()
    class ServerError : Exception()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}
