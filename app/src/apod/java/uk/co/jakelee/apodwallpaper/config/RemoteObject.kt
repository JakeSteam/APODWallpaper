package uk.co.jakelee.apodwallpaper.config

data class RemoteObject(
    val copyright: String?,
    val date: String,
    val explanation: String,
    val hdurl: String?,
    val media_type: String,
    val service_version: String,
    val title: String,
    val url: String,
    var quota: Int?
) {
    fun isValid() = this.title.isNotEmpty() && this.url.isNotEmpty()
}