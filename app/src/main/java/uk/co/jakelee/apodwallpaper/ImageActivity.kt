package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_image.*
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        val a = PreferenceHelper(this).getApodData(FileSystemHelper(this), "2018-12-14").image
        myZoomageView.setImageBitmap(a)
    }
}
