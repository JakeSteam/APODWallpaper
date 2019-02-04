package uk.co.jakelee.apodwallpaper.config

import com.google.gson.Gson
import uk.co.jakelee.apodwallpaper.api.ApiResponse

class Config: IConfig {
    override fun getUrl(auth: String, date: String) =
        "https://api.nasa.gov/planetary/apod?api_key=$auth&date=$date&hd=true"

    override fun parseResponse(response: String) = Gson().fromJson(response, ApiResponse::class.java)!!
}