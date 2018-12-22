package uk.co.jakelee.apodwallpaper.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.helper.*


class SettingsFragment: PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (activity as AppCompatActivity).supportActionBar!!.title = "Settings"
        setHasOptionsMenu(true)
        addPreferencesFromResource(R.xml.preferences_ui)
        findPreference(getString(R.string.pref_view_status)).onPreferenceClickListener = viewStatusListener
        findPreference(getString(R.string.pref_view_quota)).onPreferenceClickListener = viewQuotaListener
        findPreference(getString(R.string.pref_manually_set)).onPreferenceClickListener = manuallySetListener
        findPreference(getString(R.string.pref_notifications_instant)).onPreferenceClickListener = previewNotificationListener
        setupSeekbar(R.string.pref_automatic_check_frequency, R.integer.automatic_check_frequency_step, R.integer.automatic_check_frequency_min, R.integer.automatic_check_frequency_max)
        setupSeekbar(R.string.pref_automatic_check_variance, R.integer.automatic_check_variance_step, R.integer.automatic_check_variance_min, R.integer.automatic_check_variance_max)
        setupSeekbar(R.string.pref_filtering_width, R.integer.filtering_width_step, R.integer.filtering_width_min, R.integer.filtering_width_max)
        setupSeekbar(R.string.pref_filtering_height, R.integer.filtering_height_step, R.integer.filtering_height_min, R.integer.filtering_height_max)
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
                    TaskSchedulerHelper.cancelJobs(activity!!)
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
                    Toast.makeText(activity!!, "Invalid API key! Please try entering it again", Toast.LENGTH_SHORT).show()
                    pref.title = "No key set, tap to change"
                } else {
                    pref.title = pref.text
                }
            }
        }

    }

    private val viewStatusListener = Preference.OnPreferenceClickListener { _ ->
        val inflater = LayoutInflater.from(activity!!)
        val dialog = AlertDialog.Builder(activity!!)
            .setView(inflater.inflate(R.layout.dialog_status, null))
            .setPositiveButton("OK") { _, _ -> }
            .show()
        val prefHelper = PreferenceHelper(activity!!)
        dialog.findViewById<TextView>(R.id.last_checked)!!.text = CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_checked), true)
        dialog.findViewById<TextView>(R.id.last_pulled)!!.text = prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)
        dialog.findViewById<TextView>(R.id.last_automatic_run)!!.text = CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_run_automatic), true)
        dialog.findViewById<TextView>(R.id.last_automatic_success)!!.text = CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_set_automatic), true)
        dialog.findViewById<TextView>(R.id.last_manual_run)!!.text = CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_run_manual), true)
        dialog.findViewById<TextView>(R.id.last_manual_success)!!.text = CalendarHelper.millisToString(prefHelper.getLongPref(PreferenceHelper.LongPref.last_set_manual), true)
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
            Toast.makeText(activity, "Your API key has $remaining requests left this hour.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "There are $remaining requests remaining this hour for the default API key. Use a custom one for increased reliability!", Toast.LENGTH_SHORT).show()
        }
        true
    }

    private val manuallySetListener = Preference.OnPreferenceClickListener {
        val prefHelper = PreferenceHelper(activity!!)
        val latestPulled = prefHelper.getStringPref(PreferenceHelper.StringPref.last_pulled)
        if (!latestPulled.isNullOrEmpty()) {
            WallpaperHelper(activity!!, prefHelper).applyRequired(latestPulled!!)
        }
        true
    }

    private val previewNotificationListener = Preference.OnPreferenceClickListener {
        NotificationHelper(context!!).displayLatest()
        true
    }
}