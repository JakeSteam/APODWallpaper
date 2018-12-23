package uk.co.jakelee.apodwallpaper.api

import android.accounts.NetworkErrorException
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ApiClient(val url: String) {
    fun getApodResponse(): ApiResponse {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val quota = response.headers("X-RateLimit-Remaining")?.first()
            response.body()?.string()?.let {
                val apiResponse = Gson().fromJson(it, ApiResponse::class.java)
                return apiResponse.apply { this.quota = quota?.toIntOrNull() }
            }
            throw IOException()
        } else {
            when (response.code()) {
                400 -> throw DateRequestedException()
                429 -> throw TooManyRequestsException()
                503 -> throw TimeoutException()
                else -> throw NetworkErrorException()
            }
        }
    }

    class DateRequestedException : Exception()
    class TooManyRequestsException : Exception()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}


