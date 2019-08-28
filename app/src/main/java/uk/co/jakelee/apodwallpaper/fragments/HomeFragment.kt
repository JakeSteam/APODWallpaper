package uk.co.jakelee.apodwallpaper.fragments

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_main.*
import uk.co.jakelee.apodwallpaper.api.ApiClient
import uk.co.jakelee.apodwallpaper.api.ApiWrapper
import uk.co.jakelee.apodwallpaper.helper.*
import uk.co.jakelee.apodwallpaper.scheduling.EndpointCheckTimingHelper
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
        return inflater.inflate(uk.co.jakelee.apodwallpaper.R.layout.fragment_main, container, false)
    }

    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        hideContent()
        selectedYear = year
        selectedMonth = (month + 1)
        selectedDay = day
        getContent(
            "$selectedYear" + "-" +
                    selectedMonth.toString().padStart(2, '0') + "-" +
                    selectedDay.toString().padStart(2, '0'), false, true
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideContent()
        displayContent(PreferenceHelper(activity!!).getStringPref(PreferenceHelper.StringPref.last_pulled))
        powerSavingWarning.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivityForResult(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), 0)
            } else {
                startActivityForResult(Intent(Settings.ACTION_SETTINGS), 0)
            }
        }
        descriptionBar.setOnClickListener {
            val prefs = PreferenceHelper(activity!!)
            prefs.setBooleanPref(
                PreferenceHelper.BooleanPref.show_description,
                !prefs.getBooleanPref(PreferenceHelper.BooleanPref.show_description)
            )
            descriptionBar.setSingleLine(
                !PreferenceHelper(activity!!).getBooleanPref(
                    PreferenceHelper.BooleanPref.show_description
                )
            )
        }
    }

    private var powerSavingReceiver: BroadcastReceiver? = null

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupPowerSavingReceiver()
        }
    }

    override fun onStop() {
        super.onStop()
        powerSavingReceiver?.let { activity!!.unregisterReceiver(it) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupPowerSavingReceiver() {
        val powerManager = activity!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        togglePowerSavingBar(powerManager.isPowerSaveMode)
        powerSavingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                togglePowerSavingBar(powerManager.isPowerSaveMode)
            }
        }
        activity!!.registerReceiver(
            powerSavingReceiver,
            IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED")
        )
    }

    private fun togglePowerSavingBar(display: Boolean) {
        powerSavingWarning.visibility = if (display) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        descriptionBar.setSingleLine(!PreferenceHelper(activity!!).getBooleanPref(PreferenceHelper.BooleanPref.show_description))
        if (EndpointCheckTimingHelper.canRecheck(activity!!)) {
            getContent(EndpointCheckTimingHelper.getLatestDate(), true, true)
        }
    }

    private fun toggleRecheckIfNecessary(menuItem: MenuItem?, enabled: Boolean) = menuItem?.let {
        it.icon.alpha = if (enabled) 255 else 100
        it.isEnabled = enabled
    }

    fun getContent(
        dateString: String,
        pullingLatest: Boolean,
        manual: Boolean,
        menuItem: MenuItem? = null
    ) {
        toggleRecheckIfNecessary(menuItem, false)
        // If it's not an image, or the image exists, display the content
        if (!ContentHelper(activity!!).getContentData(dateString).isImage || FileSystemHelper(
                activity!!
            )
                .getImagePath(dateString).exists()
        ) {
            displayContent(dateString)
            toggleRecheckIfNecessary(menuItem, true)
        } else {
            disposable =
                ApiWrapper.downloadContent(activity!!, dateString, pullingLatest, manual) {}
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { toggleRecheckIfNecessary(menuItem, true) }
                    .subscribe(
                        {
                            updateSelectedDate(it.date)
                            displayContent(it.date)
                        },
                        { handleContentError(it) }
                    )
        }
    }

    private fun handleContentError(it: Throwable) {
        val errorString = when (it) {
            is ApiClient.TooManyRequestsException -> getString(uk.co.jakelee.apodwallpaper.R.string.error_quota_hit)
            is TimeoutException -> getString(uk.co.jakelee.apodwallpaper.R.string.error_no_response)
            else -> String.format(
                getString(uk.co.jakelee.apodwallpaper.R.string.error_generic_retrieval_failure),
                it.localizedMessage
            )
        }
        Toast.makeText(activity, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectedDate(dateString: String) {
        val date = CalendarHelper.stringToCalendar(dateString)
        selectedYear = date.get(Calendar.YEAR)
        selectedMonth = date.get(Calendar.MONTH) + 1
        selectedDay = date.get(Calendar.DAY_OF_MONTH)
    }

    private fun hideContent() {
        backgroundImage.setImageResource(uk.co.jakelee.apodwallpaper.R.color.colorPrimary)
        titleBar.text = activity!!.getString(uk.co.jakelee.apodwallpaper.R.string.loading_message)
        bottomButtonsGroup.visibility = View.GONE
        metadataGroup.visibility = View.GONE
    }

    private fun displayContent(dateString: String) {
        if (dateString.isNotEmpty() && activity != null) {
            val contentData = ContentHelper(activity!!).getContentData(dateString)
            titleBar.text = contentData.title
            descriptionBar.text = contentData.desc
            if (PreferenceHelper(activity!!).getStringPref(PreferenceHelper.StringPref.last_pulled) == dateString) {
                val lastChecked =
                    DateUtils.getRelativeTimeSpanString(
                        PreferenceHelper(activity!!).getLongPref(
                            PreferenceHelper.LongPref.last_checked
                        )
                    )
                metadataBar.text = String.format(
                    getString(uk.co.jakelee.apodwallpaper.R.string.metadata_bar_checked),
                    dateString,
                    lastChecked,
                    contentData.copyright
                )
            } else {
                metadataBar.text = String.format(
                    getString(uk.co.jakelee.apodwallpaper.R.string.metadata_bar),
                    dateString,
                    contentData.copyright
                )
            }
            metadataGroup.visibility = View.VISIBLE
            val image = FileSystemHelper(activity!!).getImage(contentData.date)
            if (contentData.isImage && image != null) {
                bottomButtonsGroup.visibility = View.VISIBLE
                if (image.byteCount > 100 * 1024 * 1024) {
                    Toast.makeText(
                        activity!!,
                        getString(uk.co.jakelee.apodwallpaper.R.string.error_image_too_large),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    backgroundImage.setImageBitmap(image)
                }
                fullscreenButton.setOnClickListener(
                    fullscreenButtonListener(
                        contentData.title,
                        dateString
                    )
                )
                shareButton.setOnClickListener(
                    shareButtonListener(
                        dateString,
                        contentData.title,
                        contentData.imageUrl,
                        contentData.imageUrlHd
                    )
                )
                manuallySetButton.setOnClickListener(
                    manuallySetButtonListener(
                        contentData.date,
                        image,
                        contentData.title
                    )
                )
            } else if (contentData.isImage && image == null) {
                descriptionBar.text = getString(uk.co.jakelee.apodwallpaper.R.string.error_image_not_found)
                getContent(dateString, pullingLatest = false, manual = true)
            } else {
                descriptionBar.text = String.format(
                    getString(uk.co.jakelee.apodwallpaper.R.string.apod_not_image),
                    descriptionBar.text,
                    getString(uk.co.jakelee.apodwallpaper.R.string.app_name),
                    contentData.imageUrl
                )
                backgroundImage.setImageResource(uk.co.jakelee.apodwallpaper.R.color.colorPrimary)
                bottomButtonsGroup.visibility = View.GONE
            }
        }
    }

    private fun manuallySetButtonListener(dateString: String, image: Bitmap, title: String) =
        View.OnClickListener {
            val wallpaperHelper = WallpaperHelper(activity!!, PreferenceHelper(activity!!))
            val imagePath = FileSystemHelper(activity!!).getImagePath(dateString)
            val builder = AlertDialog.Builder(activity!!)
                .setTitle(getString(uk.co.jakelee.apodwallpaper.R.string.manual_set_title))
                .setMessage(String.format(getString(uk.co.jakelee.apodwallpaper.R.string.manual_set_message), title))
                .setNegativeButton(getString(uk.co.jakelee.apodwallpaper.R.string.manual_set_wallpaper)) { _, _ ->
                    wallpaperHelper.updateWallpaper(image)
                    Toast.makeText(
                        activity!!,
                        String.format(getString(uk.co.jakelee.apodwallpaper.R.string.wallpaper_set), title),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                .setNeutralButton(getString(uk.co.jakelee.apodwallpaper.R.string.manual_set_cancel)) { _, _ -> }
            if (WallpaperHelper.canSetLockScreen()) {
                builder.setPositiveButton(getString(uk.co.jakelee.apodwallpaper.R.string.manual_set_lockscreen)) { _, _ ->
                    wallpaperHelper.updateLockScreen(imagePath)
                    Toast.makeText(
                        activity!!,
                        String.format(getString(uk.co.jakelee.apodwallpaper.R.string.lockscreen_set), title),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
            builder.show()
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
                uk.co.jakelee.apodwallpaper.R.anim.enter_from_right,
                uk.co.jakelee.apodwallpaper.R.anim.exit_to_left,
                uk.co.jakelee.apodwallpaper.R.anim.enter_from_left,
                uk.co.jakelee.apodwallpaper.R.anim.exit_to_right
            )
            .add(uk.co.jakelee.apodwallpaper.R.id.mainFrame, fragment, null)
            .addToBackStack(null)
            .commit()
    }

    private fun shareButtonListener(date: String, title: String, url: String, hdUrl: String) =
        View.OnClickListener {
            AlertDialog.Builder(activity!!)
                .setTitle(String.format(getString(uk.co.jakelee.apodwallpaper.R.string.sharing_question), title))
                .setPositiveButton(getString(uk.co.jakelee.apodwallpaper.R.string.sharing_url_hd)) { _, _ ->
                    shareUrl(
                        title,
                        hdUrl
                    )
                }
                .setNegativeButton(getString(uk.co.jakelee.apodwallpaper.R.string.sharing_url)) { _, _ -> shareUrl(title, url) }
                .setNeutralButton(getString(uk.co.jakelee.apodwallpaper.R.string.sharing_image)) { _, _ ->
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
        activity!!.startActivity(
            Intent.createChooser(
                intent,
                String.format(getString(uk.co.jakelee.apodwallpaper.R.string.sharing_title), title)
            )
        )
    }
}