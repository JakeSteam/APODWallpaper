package uk.co.jakelee.apodwallpaper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
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

        supportFragmentManager.addOnBackStackChangedListener {
            val stackHeight = supportFragmentManager.backStackEntryCount
            if (stackHeight > 0) { // if we have something on the stack (doesn't include the current shown fragment)
                supportActionBar!!.setHomeButtonEnabled(true)
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            } else {
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setHomeButtonEnabled(false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.popBackStack()
            }
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
