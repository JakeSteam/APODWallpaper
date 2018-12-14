package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!PreferenceHelper(this).haveScheduledTask()) {
            JobScheduler.scheduleJob(this)
        }

        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFrame, HomeFragment(), "HOME_FRAGMENT").commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                Toast.makeText(this, "Display some kind of settings...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_calendar -> {
                Toast.makeText(this, "Display date selector...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_recheck -> {
                val fragment = supportFragmentManager.findFragmentByTag("HOME_FRAGMENT")
                if (fragment != null && fragment.isVisible) {
                    (fragment as HomeFragment).getApod()
                }
            }
        }
        return true
    }
}
