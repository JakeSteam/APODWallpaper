package uk.co.jakelee.apodwallpaper.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.databinding.FragmentImageBinding
import java.io.File


class ImageFragment : Fragment() {
    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.title = arguments!!.getString(TITLE_ARG)
        binding.zoomageView.setImageURI(Uri.fromFile(File(arguments!!.getString(IMAGE_ARG))))
    }

    companion object {
        val TITLE_ARG = "${BuildConfig.APPLICATION_ID}.fullscreen.title"
        val IMAGE_ARG = "${BuildConfig.APPLICATION_ID}.fullscreen.image"
    }
}
