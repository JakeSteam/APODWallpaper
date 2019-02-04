package uk.co.jakelee.apodwallpaper.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.ApiWrapper
import uk.co.jakelee.apodwallpaper.helper.*
import uk.co.jakelee.apodwallpaper.scheduling.TaskExecutor
import uk.co.jakelee.apodwallpaper.scheduling.TaskTimingHelper
import java.util.*
import java.util.concurrent.TimeoutException


class HomeFragment : Fragment() {
    private var disposable: Disposable? = null
    var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        hideApod()
        selectedYear = year
        selectedMonth = (month + 1)
        selectedDay = day
        getApod(
            "$selectedYear" + "-" +
                    selectedMonth.toString().padStart(2, '0') + "-" +
                    selectedDay.toString().padStart(2, '0'), false, true
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideApod()
        displayApod(PreferenceHelper(activity!!).getStringPref(PreferenceHelper.StringPref.last_pulled))
        descriptionBar.setOnClickListener {
            val prefs = PreferenceHelper(activity!!)
            prefs.setBooleanPref(
                PreferenceHelper.BooleanPref.show_description,
                !prefs.getBooleanPref(PreferenceHelper.BooleanPref.show_description)
            )
            descriptionBar.setSingleLine(!PreferenceHelper(activity!!).getBooleanPref(PreferenceHelper.BooleanPref.show_description))
        }
    }

    override fun onResume() {
        super.onResume()
        descriptionBar.setSingleLine(!PreferenceHelper(activity!!).getBooleanPref(PreferenceHelper.BooleanPref.show_description))
        if (TaskTimingHelper.canRecheck(activity!!)) {
            getApod(TaskTimingHelper.getLatestDate(), true, true)
        }
    }

    private fun toggleRecheckIfNecessary(menuItem: MenuItem?, enabled: Boolean) = menuItem?.let {
        it.icon.alpha = if (enabled) 255 else 100
        it.isEnabled = enabled
    }

    fun getApod(dateString: String, pullingLatest: Boolean, manual: Boolean, menuItem: MenuItem? = null) {
        toggleRecheckIfNecessary(menuItem, false)
        // If it's not an image, or the image exists, display the content
        if (!PreferenceHelper(activity!!).getApodData(dateString).isImage || FileSystemHelper(activity!!)
                .getImagePath(dateString).exists()) {
            displayApod(dateString)
            toggleRecheckIfNecessary(menuItem, true)
        } else {
            disposable = ApiWrapper.downloadApod(activity!!, dateString, pullingLatest, manual) {}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { toggleRecheckIfNecessary(menuItem, true) }
                .subscribe(
                    {
                        updateSelectedDate(it.date)
                        displayApod(it.date)
                    },
                    { handleApodError(it) }
                )
        }
    }

    private fun handleApodError(it: Throwable) {
        val errorString = when (it) {
            is ApiClient.TooManyRequestsException -> getString(R.string.error_quota_hit)
            is TimeoutException -> getString(R.string.error_no_response)
            else -> String.format(getString(R.string.error_generic_retrieval_failure), it.localizedMessage)
        }
        Toast.makeText(activity, errorString, Toast.LENGTH_SHORT).show()
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
        bottomButtonsGroup.visibility = View.GONE
        metadataGroup.visibility = View.GONE
    }

    private fun displayApod(dateString: String) {
        if (dateString.isNotEmpty()) {
            val prefsHelper = PreferenceHelper(activity!!)
            val apodData = prefsHelper.getApodData(dateString)
            titleBar.text = apodData.title
            descriptionBar.text = apodData.desc
            if (prefsHelper.getStringPref(PreferenceHelper.StringPref.last_pulled) == dateString) {
                val lastChecked =
                    DateUtils.getRelativeTimeSpanString(PreferenceHelper(activity!!).getLongPref(PreferenceHelper.LongPref.last_checked))
                metadataBar.text = String.format(
                    getString(R.string.metadata_bar_checked),
                    dateString,
                    lastChecked,
                    apodData.copyright
                )
            } else {
                metadataBar.text = String.format(getString(R.string.metadata_bar), dateString, apodData.copyright)
            }
            metadataGroup.visibility = View.VISIBLE
            if (apodData.isImage) {
                bottomButtonsGroup.visibility = View.VISIBLE
                val image = FileSystemHelper(activity!!).getImage(apodData.date)
                if (image.byteCount > 100 * 1024 * 1024) {
                    Toast.makeText(activity!!, getString(R.string.error_image_too_large), Toast.LENGTH_SHORT).show()
                } else {
                    backgroundImage.setImageBitmap(image)
                }
                fullscreenButton.setOnClickListener(fullscreenButtonListener(apodData.title, dateString))
                shareButton.setOnClickListener(
                    shareButtonListener(
                        dateString,
                        apodData.title,
                        apodData.imageUrl,
                        apodData.imageUrlHd
                    )
                )
                manuallySetButton.setOnClickListener(manuallySetButtonListener(apodData.date, image, apodData.title))
            } else {
                descriptionBar.text = String.format(
                    getString(R.string.apod_not_image),
                    descriptionBar.text,
                    getString(R.string.app_name),
                    apodData.imageUrl
                )
                backgroundImage.setImageResource(R.color.colorPrimary)
                bottomButtonsGroup.visibility = View.GONE
            }
        }
    }

    private fun manuallySetButtonListener(dateString: String, image: Bitmap, title: String) = View.OnClickListener {
        val wallpaperHelper = WallpaperHelper(activity!!, PreferenceHelper(activity!!))
        val imagePath = FileSystemHelper(activity!!).getImagePath(dateString)
        AlertDialog.Builder(activity!!)
            .setTitle(getString(R.string.manual_set_title))
            .setMessage(String.format(getString(R.string.manual_set_message), title))
            .setPositiveButton(getString(R.string.manual_set_lockscreen)) { _, _ ->
                wallpaperHelper.updateLockScreen(imagePath)
                Toast.makeText(activity!!, String.format(getString(R.string.lockscreen_set), title), Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton(getString(R.string.manual_set_wallpaper)) { _, _ ->
                wallpaperHelper.updateWallpaper(image)
                Toast.makeText(activity!!, String.format(getString(R.string.wallpaper_set), title), Toast.LENGTH_SHORT)
                    .show()
            }
            .setNeutralButton(getString(R.string.manual_set_cancel)) { _, _ -> }
            .show()
    }

    private fun fullscreenButtonListener(title: String, dateString: String) = View.OnClickListener {
        val imageFile = FileSystemHelper(activity!!).getImagePath(dateString)
        val bundle = Bundle().apply {
            putString(ImageFragment.IMAGE_ARG, imageFile.path)
            putString(ImageFragment.TITLE_ARG, title)
        }
        val fragment = ImageFragment().apply { arguments = bundle }
        activity!!.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right,
                R.anim.exit_to_left,
                R.anim.enter_from_left,
                R.anim.exit_to_right
            )
            .add(R.id.mainFrame, fragment, null)
            .addToBackStack(null)
            .commit()
    }

    private fun shareButtonListener(date: String, title: String, url: String, hdUrl: String) = View.OnClickListener {
        AlertDialog.Builder(activity!!)
            .setTitle(String.format(getString(R.string.sharing_question), title))
            .setPositiveButton(getString(R.string.sharing_url_hd)) { _, _ -> shareUrl(title, hdUrl) }
            .setNegativeButton(getString(R.string.sharing_url)) { _, _ -> shareUrl(title, url) }
            .setNeutralButton(getString(R.string.sharing_image)) { _, _ ->
                FileSystemHelper(activity!!).shareImage(
                    date,
                    title
                )
            }
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