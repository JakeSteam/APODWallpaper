package uk.co.jakelee.apodwallpaper.config

// science.nasa.gov/wp-json/wp/v2/apod-basic. Gson ignores Kotlin nullability, so anything the
// endpoint might omit is nullable. `url` is deliberately absent: it is now the article page, not
// an image, and duplicates `permalink`.
data class RemoteApod(
    val date: String?,
    val title: String?,
    val explanation: String?,
    val media_type: String?,
    val hdurl: String?,
    val copyright: String?,
    val credit: String?,
    val permalink: String?
) {
    fun isValid() = !this.date.isNullOrEmpty() && !this.title.isNullOrEmpty()
}
