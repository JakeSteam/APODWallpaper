package uk.co.jakelee.apodwallpaper.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import uk.co.jakelee.apodwallpaper.BuildConfig
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.helper.*
import java.text.SimpleDateFormat
import java.util.*


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view!!.setBackgroundColor(Color.WHITE)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (activity as AppCompatActivity).supportActionBar!!.title = getString(R.string.settings_title)
        setHasOptionsMenu(true)
        addPreferencesFromResource(R.xml.preferences_ui)
        findPreference(getString(R.string.pref_view_status)).onPreferenceClickListener = viewStatusListener
        findPreference(getString(R.string.pref_view_quota)).onPreferenceClickListener = viewQuotaListener
        findPreference(getString(R.string.pref_notifications_instant)).onPreferenceClickListener =
                previewNotificationListener
        findPreference(getString(R.string.pref_delete_images)).onPreferenceClickListener = deleteImagesListener
        findPreference(getString(R.string.pref_feedback)).onPreferenceClickListener = giveFeedbackListener
        findPreference(getString(R.string.pref_version)).title = "V${BuildConfig.VERSION_NAME}"
        findPreference(getString(R.string.pref_version)).summary = String.format(getString(R.string.version_summary),
            BuildConfig.VERSION_CODE,
            SimpleDateFormat("dd MMM yyy", Locale.US).format(BuildConfig.BUILD_TIME))
        setupSeekbar(
            R.string.pref_automatic_check_frequency,
            R.integer.automatic_check_frequency_step,
            R.integer.automatic_check_frequency_min,
            R.integer.automatic_check_frequency_max
        )
        setupSeekbar(
            R.string.pref_automatic_check_variance,
            R.integer.automatic_check_variance_step,
            R.integer.automatic_check_variance_min,
            R.integer.automatic_check_variance_max
        )
        setupSeekbar(
            R.string.pref_filtering_width,
            R.integer.filtering_width_step,
            R.integer.filtering_width_min,
            R.integer.filtering_width_max
        )
        setupSeekbar(
            R.string.pref_filtering_height,
            R.integer.filtering_height_step,
            R.integer.filtering_height_min,
            R.integer.filtering_height_max
        )
        setupSeekbar(
            R.string.pref_filtering_ratio,
            R.integer.filtering_ratio_step,
            R.integer.filtering_ratio_min,
            R.integer.filtering_ratio_max
        )
        val customKeyPref = (findPreference(getString(R.string.pref_custom_key)) as EditTextPreference)
        if (customKeyPref.text.isNotEmpty()) {
            customKeyPref.title = customKeyPref.text
        }
    }

    fun setupSeekbar(id: Int, step: Int, min: Int, max: Int) {
        val seekbar = findPreference(getString(id)) as SeekBarPreference
        seekbar.seekBarIncrement = resources.getInteger(step)
        seekbar.min = resources.getInteger(min)
        seekbar.max = resources.getInteger(max)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref = findPreference(key)
        when {
            key == getString(R.string.pref_automatic_enabled) && pref is SwitchPreference -> {
                if (pref.isChecked) {
                    TaskSchedulerHelper.scheduleJob(activity!!)
                } else {
                    TaskSchedulerHelper.cancelJob(activity!!)
                }
            }
            key == getString(R.string.pref_automatic_check_wifi)
                    || key == getString(R.string.pref_automatic_check_frequency)
                    || key == getString(R.string.pref_automatic_check_variance) -> {
                TaskSchedulerHelper.scheduleJob(activity!!)
            }
            key == getString(R.string.pref_custom_key) && pref is EditTextPreference -> {
                if (pref.text.length < 40) {
                    pref.text = ""
                    Toast.makeText(activity!!, getString(R.string.error_invalid_api_key), Toast.LENGTH_SHORT).show()
                    pref.title = getString(R.string.no_api_key_set)
                } else {
                    pref.title = pref.text
                }
            }
        }
    }

    private val deleteImagesListener = Preference.OnPreferenceClickListener {
        FileSystemHelper(context!!).deleteAllPastImages()
        Toast.makeText(context!!, getString(R.string.deleted_all_images), Toast.LENGTH_SHORT).show()
        true
    }

    private val giveFeedbackListener = Preference.OnPreferenceClickListener {
        AlertDialog.Builder(activity!!)
            .setTitle(getString(R.string.give_feedback_title))
            .setPositiveButton(getString(R.string.give_feedback_playstore)) { _, _ ->
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}"))
                )
            }
            .setNegativeButton(getString(R.string.give_feedback_github)) { _, _ ->
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(getString(R.string.repo_url) + "/issues"))
                )
            }
            .setNeutralButton(getString(R.string.give_feedback_email)) { _, _ ->
                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.data = Uri.parse(getString(R.string.give_feedback_email_address))
                startActivity(Intent.createChooser(emailIntent, getString(R.string.give_feedback_email_title)))
            }
            .show()
        true
    }

    private val viewStatusListener = Preference.OnPreferenceClickListener { _ ->
        val inflater = LayoutInflater.from(activity!!)
        val dialog = AlertDialog.Builder(activity!!)
            .setView(inflater.inflate(R.layout.dialog_status, null))
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
        val prefHelper = PreferenceHelper(activity!!)
        dialog.findViewById<TextView>(R.id.last_checked)!!.text =
                CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_checked), true)
        dialog.findViewById<TextView>(R.id.last_pulled)!!.text =
                prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)
        dialog.findViewById<TextView>(R.id.last_automatic_run)!!.text = CalendarHelper.millisToString(
            prefHelper.getLongPref(PreferenceHelper.LongPref.last_run_automatic),
            true
        )
        dialog.findViewById<TextView>(R.id.last_automatic_success)!!.text = CalendarHelper.millisToString(
            prefHelper.getLongPref(PreferenceHelper.LongPref.last_set_automatic),
            true
        )
        dialog.findViewById<TextView>(R.id.last_manual_run)!!.text =
                CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_run_manual), true)
        dialog.findViewById<TextView>(R.id.last_manual_success)!!.text =
                CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_set_manual), true)
        dialog.findViewById<TextView>(R.id.last_filtered_date)!!.text =
                prefHelper.getStringPref(PreferenceHelper.StringPref.last_filtered_date)
        dialog.findViewById<TextView>(R.id.last_filtered_reason)!!.text =
                prefHelper.getStringPref(PreferenceHelper.StringPref.last_filtered_reason)
        var count = 0
        var size = 0L
        FileSystemHelper(activity!!).getImagesDirectory().listFiles().forEach {
            count++
            size += it.length()
        }
        dialog.findViewById<TextView>(R.id.images_saved)!!.text = count.toString()
        dialog.findViewById<TextView>(R.id.images_cache)!!.text = Formatter.formatFileSize(activity!!, size)
        true
    }

    private val viewQuotaListener = Preference.OnPreferenceClickListener {
        val prefHelper = PreferenceHelper(activity!!)
        val remaining = prefHelper.getIntPref(PreferenceHelper.IntPref.api_quota)
        if (prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.custom_key_enabled)) {
            Toast.makeText(
                activity,
                String.format(getString(R.string.api_key_custom_remaining), remaining),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                activity,
                String.format(getString(R.string.api_key_remaining), remaining),
                Toast.LENGTH_SHORT
            ).show()
        }
        true
    }

    private val previewNotificationListener = Preference.OnPreferenceClickListener {
        NotificationHelper(context!!).displayLatest()
        true
    }
}