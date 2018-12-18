package uk.co.jakelee.apodwallpaper.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.helper.TaskSchedulerHelper
import java.util.*


class HomeFragment : Fragment() {
    private var disposable: Disposable? = null
    var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        hideApod()
        selectedYear = year
        selectedMonth = (month + 1)
        selectedDay = day
        getApod("$selectedYear" + "-" +
                selectedMonth.toString().padStart(2, '0') + "-" +
                selectedDay.toString().padStart(2, '0'), false, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.title = getString(R.string.app_name)
        hideApod()
        displayApod(PreferenceHelper(activity!!).getStringPref(PreferenceHelper.StringPref.last_pulled))
        if (TaskSchedulerHelper.canRecheck(activity!!)) {
            getApod(TaskSchedulerHelper.getLatestDate(), true, true)
        }
        descriptionBar.setOnClickListener {
            val prefs = PreferenceHelper(activity!!)
            prefs.setBooleanPref(PreferenceHelper.BooleanPref.show_description, !prefs.getBooleanPref(PreferenceHelper.BooleanPref.show_description))
            descriptionBar.setSingleLine(!PreferenceHelper(activity!!).getBooleanPref(PreferenceHelper.BooleanPref.show_description))
        }
    }

    override fun onResume() {
        super.onResume()
        descriptionBar.setSingleLine(!PreferenceHelper(activity!!).getBooleanPref(PreferenceHelper.BooleanPref.show_description))
    }

    private fun toggleRecheckIfNecessary(menuItem: MenuItem?, enabled: Boolean) = menuItem?.let {
            it.icon.alpha = if (enabled) 255 else 100
            it.isEnabled = enabled
        }

    private var checkedPreviousDay = false
    fun getApod(dateString: String, pullingLatest: Boolean, manual: Boolean, menuItem: MenuItem? = null) {
        toggleRecheckIfNecessary(menuItem, false)
        if (FileSystemHelper(activity!!).getImage(dateString).exists()) {
            displayApod(dateString)
            toggleRecheckIfNecessary(menuItem, true)
            Toast.makeText(activity, "APOD already exists, no need to recheck!", Toast.LENGTH_SHORT).show()
        } else {
            disposable = TaskSchedulerHelper.downloadApod(activity!!, dateString, pullingLatest, manual)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { toggleRecheckIfNecessary(menuItem, true) }
                .subscribe(
                    {
                        updateSelectedDate(dateString)
                        displayApod(dateString)
                    },
                    {
                        Timber.e(it)
                        if (pullingLatest && it is ApiClient.DateRequestedException && !checkedPreviousDay) {
                            checkedPreviousDay = true
                            val newDateString = CalendarHelper.modifyStringDate(dateString, -1)
                            Toast.makeText(activity, "Failed to find APOD for $dateString, trying $newDateString", Toast.LENGTH_SHORT).show()
                            updateSelectedDate(newDateString)
                            getApod(newDateString, true, true)
                        } else {
                            Toast.makeText(activity, "Unknown server error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
        }
    }

    private fun updateSelectedDate(dateString: String) {
        val date = CalendarHelper.stringToCalendar(dateString)
        selectedYear = date.get(Calendar.YEAR)
        selectedMonth = date.get(Calendar.MONTH) + 1
        selectedDay = date.get(Calendar.DAY_OF_MONTH)
    }

    private fun hideApod() {
        backgroundImage.setImageResource(R.color.colorPrimary)
        titleBar.text = activity!!.getString(R.string.loading_message)
        metadataGroup.visibility = View.GONE
    }

    private fun displayApod(dateString: String) {
        if (dateString.isNotEmpty()) {
            val prefsHelper = PreferenceHelper(activity!!)
            val apodData = prefsHelper.getApodData(FileSystemHelper(activity!!), dateString)
            backgroundImage.setImageBitmap(apodData.image)
            titleBar.text = apodData.title
            descriptionBar.text = apodData.desc
            fullscreenButton.setOnClickListener(fullscreenButtonListener(apodData.title, dateString))
            shareButton.setOnClickListener(shareButtonListener(dateString, apodData.title, apodData.imageUrl, apodData.imageUrlHd))
            if (prefsHelper.getStringPref(PreferenceHelper.StringPref.last_pulled) == dateString) {
                val lastChecked = DateUtils.getRelativeTimeSpanString(PreferenceHelper(activity!!).getLongPref(PreferenceHelper.LongPref.last_checked))
                metadataBar.text = String.format(getString(R.string.metadata_bar_checked), dateString, lastChecked, apodData.copyright)
            } else {
                metadataBar.text = String.format(getString(R.string.metadata_bar), dateString, apodData.copyright)
            }
            metadataGroup.visibility = View.VISIBLE
        }
    }


    private fun fullscreenButtonListener(title: String, dateString: String) = View.OnClickListener {
        val imageFile = FileSystemHelper(activity!!).getImage(dateString)
        val bundle = Bundle().apply {
            putString("image", imageFile.path)
            putString("title", title)
        }
        val fragment = ImageFragment().apply { arguments = bundle }
        activity!!.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right,
                R.anim.exit_to_left,
                R.anim.enter_from_left,
                R.anim.exit_to_right
            )
            .replace(R.id.mainFrame, fragment, "image_fragment")
            .addToBackStack(null)
            .commit()
    }

    private fun shareButtonListener(date: String, title: String, url: String, hdUrl: String) = View.OnClickListener {
        AlertDialog.Builder(activity!!)
            .setTitle(String.format(getString(R.string.sharing_question), title))
            .setPositiveButton(getString(R.string.sharing_url_hd)) { _, _ -> shareUrl(title, hdUrl)}
            .setNegativeButton(getString(R.string.sharing_url)) { _, _ -> shareUrl(title, url)}
            .setNeutralButton(getString(R.string.sharing_image)) { _, _ -> FileSystemHelper(activity!!).shareImage(date, title)}
            .show()
    }

    private fun shareUrl(title: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
        }
        activity!!.startActivity(Intent.createChooser(intent, String.format(getString(R.string.sharing_title), title)))
    }
}