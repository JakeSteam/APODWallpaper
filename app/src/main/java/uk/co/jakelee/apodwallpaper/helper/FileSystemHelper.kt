package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

class FileSystemHelper(val context: Context) {
    fun saveImage(bitmap: Bitmap, date: String) {
        val filePath = File(context.filesDir, "images")
        filePath.mkdirs()
        val stream = FileOutputStream("$filePath/$date.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    }

    fun getImage(date: String) = File(File(context.filesDir, "images"), "$date.png")
}