package uk.co.jakelee.apodwallpaper.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        resetApod()
        selectedYear = year
        selectedMonth = (month + 1)
        selectedDay = day
        getApod("$selectedYear" + "-" +
                selectedMonth.toString().padStart(2, '0') + "-" +
                selectedDay.toString().padStart(2, '0'), false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.title = getString(R.string.app_name)
        resetApod()
        displayApod(PreferenceHelper(activity!!).getLastPulledDate())
        if (TaskSchedulerHelper.canRecheck(activity!!)) {
            getApod(TaskSchedulerHelper.getLatestDate(), true)
        }
    }

    private fun setUpFullscreenButton(title: String, dateString: String){
        fullscreenButton.setOnClickListener {
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
    }

    fun getApod(dateString: String, pullingLatest: Boolean) {
        if (PreferenceHelper(activity!!).doesDataExist(activity!!, dateString)) {
            displayApod(dateString)
        } else {
            disposable = TaskSchedulerHelper.downloadApod(
                activity!!,
                dateString,
                pullingLatest
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        updateSelectedDate(dateString)
                        displayApod(dateString)
                    },
                    {
                        Timber.e(it)
                        if (pullingLatest && it is ApiClient.DateRequestedException) {
                            val newDateString = CalendarHelper.modifyStringDate(dateString, -1)
                            Toast.makeText(activity, "Failed to find APOD for $dateString, trying $newDateString", Toast.LENGTH_SHORT).show()
                            updateSelectedDate(newDateString)
                            getApod(newDateString, true)
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

    private fun resetApod() {
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
            setUpFullscreenButton(apodData.title, dateString)
            if (prefsHelper.getLastPulledDate() == dateString) {
                val lastChecked = DateUtils.getRelativeTimeSpanString(PreferenceHelper(activity!!).getLastCheckedDate())
                metadataBar.text = String.format(getString(R.string.metadata_bar_checked), dateString, lastChecked, apodData.copyright)
            } else {
                metadataBar.text = String.format(getString(R.string.metadata_bar), dateString, apodData.copyright)
            }
            metadataGroup.visibility = View.VISIBLE
        }
    }
}