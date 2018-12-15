package uk.co.jakelee.apodwallpaper.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import uk.co.jakelee.apodwallpaper.R

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (activity as AppCompatActivity).supportActionBar!!.title = "Settings"
        setHasOptionsMenu(true)
        addPreferencesFromResource(R.xml.preferences)
        findPreference("view_log").setOnPreferenceClickListener {
            Toast.makeText(activity, "View log...", Toast.LENGTH_SHORT).show()
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