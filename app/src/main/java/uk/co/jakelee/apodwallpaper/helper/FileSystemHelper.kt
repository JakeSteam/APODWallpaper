package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import java.io.File
import java.io.FileOutputStream

class FileSystemHelper(private val context: Context) {
    private val imagesDir = "images"
    private val suffix = ".png"

    fun saveImage(bitmap: Bitmap, date: String) {
        val filePath = File(context.cacheDir, imagesDir)
        filePath.mkdirs()
        val stream = FileOutputStream("$filePath/$date$suffix")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    }

    fun getImagesDirectory(): File {
        val target = File(context.cacheDir, imagesDir)
        if (!target.exists()) {
            target.mkdirs()
        }
        return target
    }

    fun getImagePath(date: String) = File(getImagesDirectory(), "$date$suffix")

    fun getImage(date: String) = BitmapFactory.decodeFile(getImagePath(date).path)

    fun shareImage(date: String, title: String) {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        FileProvider.getUriForFile(context, authority, getImagePath(date))?.let {
            val shareIntent = Intent()
                .setAction(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setDataAndType(it, context.contentResolver.getType(it))
                .putExtra(Intent.EXTRA_STREAM, it)
                .putExtra(Intent.EXTRA_TEXT, title)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    String.format(context.getString(R.string.share_title), title)
                )
            )
        }
    }

    fun deleteAllPastImages() {
        val lastPulled = PreferenceHelper(context).getStringPref(PreferenceHelper.StringPref.last_pulled)
        val files = getImagesDirectory().listFiles { dir, filename -> !filename.startsWith(lastPulled) }
        files.forEach {
            it.delete()
        }
    }
}