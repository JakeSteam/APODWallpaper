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
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.helper.TaskSchedulerHelper


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
        val customKeyPref = (findPreference(getString(R.string.pref_custom_key)) as EditTextPreference)
        if (customKeyPref.text.isNotEmpty()) {
            customKeyPref.title = customKeyPref.text
        }
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
        Toast.makeText(activity, "View quota...", Toast.LENGTH_SHORT).show()
        true
    }
}