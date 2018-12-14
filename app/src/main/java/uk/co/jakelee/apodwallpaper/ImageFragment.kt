package uk.co.jakelee.apodwallpaper

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_image.*
import java.io.File


class ImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //activity!!.actionBar.setDisplayHomeAsUpEnabled(true)
        val imagePath = arguments!!.getString("image")
        myZoomageView.setImageURI(Uri.fromFile(File(imagePath)))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->  activity!!.supportFragmentManager.beginTransaction().remove(this).commit()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        val TAG = "IMAGE_FRAGMENT"
    }
}
