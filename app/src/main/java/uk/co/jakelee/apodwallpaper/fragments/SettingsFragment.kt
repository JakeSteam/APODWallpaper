package uk.co.jakelee.apodwallpaper.fragments

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import uk.co.jakelee.apodwallpaper.R
import uk.co.jakelee.apodwallpaper.helper.CalendarHelper
import uk.co.jakelee.apodwallpaper.helper.FileSystemHelper
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (activity as AppCompatActivity).supportActionBar!!.title = "Settings"
        setHasOptionsMenu(true)
        addPreferencesFromResource(R.xml.preferences)
        findPreference(getString(R.string.view_status)).setOnPreferenceClickListener { _ ->
            val inflater = LayoutInflater.from(activity!!)
            val dialog = AlertDialog.Builder(activity!!)
                .setView(inflater.inflate(R.layout.dialog_status, null))
                .setPositiveButton("OK") { _, _ -> }
                .show()
            val prefHelper = PreferenceHelper(activity!!)
            dialog.findViewById<TextView>(R.id.last_checked)!!.text = CalendarHelper.millisToString(prefHelper.getLastCheckedDate(), true)
            dialog.findViewById<TextView>(R.id.last_pulled)!!.text = prefHelper.getLastPulledDateString()
            dialog.findViewById<TextView>(R.id.last_automatic_run)!!.text = CalendarHelper.millisToString(prefHelper.getLastRunDate(false), true)
            dialog.findViewById<TextView>(R.id.last_automatic_success)!!.text = CalendarHelper.millisToString(prefHelper.getLastSetDate(false), true)
            dialog.findViewById<TextView>(R.id.last_manual_run)!!.text = CalendarHelper.millisToString(prefHelper.getLastRunDate(true), true)
            dialog.findViewById<TextView>(R.id.last_manual_success)!!.text = CalendarHelper.millisToString(prefHelper.getLastSetDate(true), true)

            var count = 0
            var size = 0L
            FileSystemHelper(activity!!).getImageDirectory().listFiles().forEach {
                count++
                size += it.length()
            }
            dialog.findViewById<TextView>(R.id.images_saved)!!.text = count.toString()
            dialog.findViewById<TextView>(R.id.images_cache)!!.text = Formatter.formatFileSize(activity!!, size)
            true
        }
        findPreference("view_quota").setOnPreferenceClickListener {
            Toast.makeText(activity, "View quota...", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}