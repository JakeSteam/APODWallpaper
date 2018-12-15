package uk.co.jakelee.apodwallpaper.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ApiClient(val url: String) {
    fun getApodResponse(): ResponseApod {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = OkHttpClient().newCall(request).execute()
        if (response.isSuccessful) {
            response.body()?.string()?.let {
                return Gson().fromJson(it, ResponseApod::class.java)
            }
            throw IOException()
        } else {
            throw DateRequestedException()
        }
    }

    class DateRequestedException() : Exception()
}


