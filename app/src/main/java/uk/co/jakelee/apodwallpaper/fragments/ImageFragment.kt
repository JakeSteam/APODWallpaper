package uk.co.jakelee.apodwallpaper.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_image.*
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import java.io.File


class ImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.title = arguments!!.getString(TITLE_ARG)
        zoomageView.setImageURI(Uri.fromFile(File(arguments!!.getString(IMAGE_ARG))))
    }

    companion object {
        val TITLE_ARG = "${BuildConfig.APPLICATION_ID}.fullscreen.title"
        val IMAGE_ARG = "${BuildConfig.APPLICATION_ID}.fullscreen.image"
    }
}
