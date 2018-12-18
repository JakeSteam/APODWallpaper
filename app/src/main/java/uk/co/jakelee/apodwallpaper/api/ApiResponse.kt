package uk.co.jakelee.apodwallpaper.api

data class ApiResponse(
    val copyright: String?,
    val date: String,
    val explanation: String,
    val hdurl: String?,
    val media_type: String,
    val service_version: String,
    val title: String,
    val url: String
) {

    fun isValid() =
        this.media_type == "image" && this.title.isNotEmpty() && !this.url.isEmpty()
}