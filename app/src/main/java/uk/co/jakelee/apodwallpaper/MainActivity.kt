package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
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

    private fun getApod(date: Date) {
        val dateString = SimpleDateFormat("yyyy-MM-yy", Locale.UK).format(date)
        val url = "https://api.nasa.gov/planetary/apod?api_key=${BuildConfig.APOD_API_KEY}&date=$dateString&hd=true"
        Timber.d("Pulling data...")
        disposable = Single
            .fromCallable {
                getResponse(url)
            }
            .subscribe(
                {
                    if (checkValidResults(it)) {
                        saveResults(it)
                        updateWallpaper(it)
                    }
                },
                { Timber.e(it) }
            )
    }

    private fun checkValidResults(response: ApodResponse) =
        response.media_type == "image" && response.title.isNotEmpty() && response.hdurl.isNotEmpty()

    private fun saveResults(response: ApodResponse) {
        Toast.makeText(this, "Received title: ${response.title}", Toast.LENGTH_SHORT).show()
    }

    private fun updateWallpaper(response: ApodResponse) {
        Toast.makeText(this, "Setting wallpaper: ${response.hdurl}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

}
