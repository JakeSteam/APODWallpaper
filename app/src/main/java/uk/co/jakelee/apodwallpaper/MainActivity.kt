package uk.co.jakelee.apodwallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*




class MainActivity : AppCompatActivity() {
    var disposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        testPull.setOnClickListener {
            getApod(Calendar.getInstance().time)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                Toast.makeText(this, "Display some kind of settings...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_calendar -> {
                Toast.makeText(this, "Display date selector...", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    private fun getApod(date: Date) {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(date)
        val url = "https://api.nasa.gov/planetary/apod?api_key=${BuildConfig.APOD_API_KEY}&date=$dateString&hd=true"
        disposable = Single
            .fromCallable { getResponse(url) }
            .map {
                val a = getImage(it.hdurl ?: it.url)
                intermediate(it, a)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (checkValidResults(it.response)) {
                        saveResults(it.response, it.image, dateString)
                        updateWallpaper(it.image)
                    }
                },
                { Timber.e(it) }
            )
    }

    data class intermediate(val response: ApodResponse, val image: Bitmap) {

    }

    private fun checkValidResults(response: ApodResponse) =
        response.media_type == "image" && response.title.isNotEmpty() &&
                (!response.hdurl.isNullOrEmpty() || !response.url.isEmpty())

    private fun saveResults(response: ApodResponse, image: Bitmap, dateString: String) {
        val imageUrl = response.hdurl ?: response.url
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString("${dateString}_title", response.title)
            .putString("${dateString}_desc", response.explanation)
            .putString("${dateString}_image", imageUrl)
            .apply()
        saveToInternal(this, image, dateString)
    }

    private fun saveToInternal(context: Context, bitmap: Bitmap, date: String) {
        val filePath = File(context.filesDir, "images")
        filePath.mkdirs()
        val stream = FileOutputStream("$filePath/$date.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        val file = File(File(context.filesDir, "images"), "$date.png")
    }

    private fun updateWallpaper(bitmap: Bitmap) {
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        wallpaperManager.setBitmap(bitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

}
