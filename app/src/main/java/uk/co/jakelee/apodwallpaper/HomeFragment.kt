package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper



class HomeFragment : Fragment() {
    var disposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayLatestSavedApod()
    }

    private fun displayLatestSavedApod() {
        val lastPulled = PreferenceHelper(activity!!).getLastPulledDate()
        if (lastPulled != "") {
            val lastChecked = DateUtils.getRelativeTimeSpanString(PreferenceHelper(activity!!).getLastCheckedDate())
            val apodData = PreferenceHelper(activity!!).getApodData(FileSystemHelper(activity!!), lastPulled)
            backgroundImage.setImageBitmap(apodData.image)
            titleBar.text = apodData.title
            descriptionBar.text = apodData.desc
            metadataBar.text = String.format(getString(R.string.last_checked), lastPulled, lastChecked)
            setUpFullscreenButton(lastPulled)
        }
    }

    private fun setUpFullscreenButton(dateString: String) = fullscreenButton.setOnClickListener {
        val imageFile = FileSystemHelper(activity!!).getImage(dateString)
        val bundle = Bundle().apply { putString("image", imageFile.path) }
        val fragment = ImageFragment().apply { arguments = bundle }
        activity!!.supportFragmentManager.beginTransaction()
            .replace(R.id.mainFrame, fragment, "image_fragment")
            .addToBackStack(null)
            .commit()
    }

    fun getApod(dateString: String = JobScheduler.getLatestDate()) {
        disposable = JobScheduler.downloadApod(activity!!, dateString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { displayLatestSavedApod() },
                { Timber.e(it) }
            )
    }
}