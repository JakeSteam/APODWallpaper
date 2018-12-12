package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        testPull.setOnClickListener {
            getApod(Calendar.getInstance().time)
        }
    }

    private fun getApod(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-yy", Locale.UK)
        val dateString = dateFormat.format(date)
        val url = "https://api.nasa.gov/planetary/apod?api_key=${BuildConfig.APOD_API_KEY}&date=$dateString&hd=true"
        Log.d("app", url)
        // perform network request
        // parse results
        // store locally
        // update wallpapers
    }
}
