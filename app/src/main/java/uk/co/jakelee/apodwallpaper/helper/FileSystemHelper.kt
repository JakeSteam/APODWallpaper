package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class FileSystemHelper(private val context: Context) {
    fun saveImage(bitmap: Bitmap, date: String) {
        val filePath = File(context.cacheDir, "images")
        filePath.mkdirs()
        val stream = FileOutputStream("$filePath/$date.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    }

    fun getImageDirectory() = File(context.cacheDir, "images")

    fun getImage(date: String) = File(getImageDirectory(), "$date.png")

    fun shareImage(date: String, title: String) {
        val contentUri = FileProvider.getUriForFile(context, "uk.co.jakelee.apodwallpaper.fileprovider", getImage(date))
        if (contentUri != null) {
            // Share via content provider, giving receiver permission to read stream
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.putExtra(Intent.EXTRA_TEXT, title)
            context.startActivity(Intent.createChooser(shareIntent, "Share \"$title\" to..."))
        }
    }
}