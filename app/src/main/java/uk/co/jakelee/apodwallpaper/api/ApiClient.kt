package uk.co.jakelee.apodwallpaper.api

import android.content.res.Resources
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.co.jakelee.apodwallpaper.config.ApodResponse
import uk.co.jakelee.apodwallpaper.config.Config
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ApiClient(val url: String) {

    fun getApodResponse(): ApodResponse {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val quota = response.headers("X-RateLimit-Remaining")?.firstOrNull()
            response.body()?.string()?.let {
                val apiResponse = Config().parseResponse(it)
                return apiResponse.apply { this.quota = quota?.toIntOrNull() }
            }
            throw IOException()
        } else {
            when (response.code()) {
                400 -> throw DateRequestedException()
                404 -> throw Resources.NotFoundException()
                429 -> throw TooManyRequestsException()
                500 -> throw ServerError()
                503 -> throw TimeoutException()
                else -> throw UnknownError()
            }
        }
    }

    class DateRequestedException : Exception()
    class TooManyRequestsException : Exception()
    class ServerError : Exception()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}


