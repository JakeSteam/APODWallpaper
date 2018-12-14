package uk.co.jakelee.apodwallpaper

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File


class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        val imagePath = intent.getStringExtra("image")
        myZoomageView.setImageURI(Uri.fromFile(File(imagePath)))
    }
}
