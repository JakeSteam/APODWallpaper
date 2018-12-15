package uk.co.jakelee.apodwallpaper

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val HomeFragmentTag = "HOME_FRAGMENT"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!PreferenceHelper(this).haveScheduledTask()) {
            JobScheduler.scheduleJob(this)
        }

        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFrame, HomeFragment(), HomeFragmentTag).commit()

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
        val fragment = supportFragmentManager.findFragmentByTag(HomeFragmentTag)
        if (fragment != null && fragment.isVisible) {
            (fragment as HomeFragment)
            when (item.itemId) {
                R.id.nav_settings -> {
                    Toast.makeText(this, "Display some kind of settings...", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_calendar -> {
                    DatePickerDialog(this, fragment.dateSetListener,
                        fragment.selectedYear,
                        fragment.selectedMonth,
                        fragment.selectedDay
                    ).show()
                }
                R.id.nav_recheck -> {
                    if (System.currentTimeMillis() - PreferenceHelper(this).getLastCheckedDate() > TimeUnit.MINUTES.toMillis(10)) {
                        fragment.getApod(JobScheduler.getLatestDate(), true)
                    } else {
                        Toast.makeText(this, "Checked too recently, please try again in a few minutes!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (item.itemId == android.R.id.home) {
            supportFragmentManager.popBackStack()
        }
        return true
    }
}
