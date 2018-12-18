package uk.co.jakelee.apodwallpaper.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(val url: String) {
    fun getApodResponse(): ApiResponse {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response.body()?.string()?.let {
                return Gson().fromJson(it, ApiResponse::class.java)
            }
            throw IOException()
        } else {
            throw DateRequestedException()
        }
    }

    class DateRequestedException : Exception()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}


