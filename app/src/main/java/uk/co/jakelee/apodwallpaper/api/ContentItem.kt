package uk.co.jakelee.apodwallpaper.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import uk.co.jakelee.apodwallpaper.config.Config
import java.net.URL

data class ContentItem(
    val date: String,
    val title: String,
    val desc: String,
    val imageUrl: String,
    val imageUrlHd: String,
    val copyright: String,
    val isImage: Boolean
) {

    fun pullRemoteImage(useHd: Boolean): Bitmap {
        val url = if (useHd && this.imageUrlHd.isNotEmpty()) this.imageUrlHd else this.imageUrl
        return BitmapFactory.decodeStream(URL(url).openStream())
    }
}