package uk.co.jakelee.apodwallpaper.api

import android.graphics.*
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

    fun pullImageFromServer(useHd: Boolean): Bitmap {
        val url = if (useHd && this.imageUrlHd.isNotEmpty()) this.imageUrlHd else this.imageUrl
        return BitmapFactory.decodeStream(URL(url).openStream())
    }

    fun greyscaleImage(img: Bitmap): Bitmap {
        val bmpGreyscale = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
        val colorMatrixFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        Canvas(bmpGreyscale).drawBitmap(img, 0f, 0f, Paint().apply { colorFilter = colorMatrixFilter })
        return bmpGreyscale
    }
}