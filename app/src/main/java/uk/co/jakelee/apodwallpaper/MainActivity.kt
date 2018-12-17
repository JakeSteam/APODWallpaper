package uk.co.jakelee.apodwallpaper

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uk.co.jakelee.apodwallpaper.fragments.HomeFragment
import uk.co.jakelee.apodwallpaper.fragments.SettingsFragment
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.helper.TaskSchedulerHelper
import java.util.*

class MainActivity : AppCompatActivity() {

    private val HomeFragmentTag = "HOME_FRAGMENT"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!PreferenceHelper(this).haveScheduledTask()) {
            TaskSchedulerHelper.scheduleJob(this)
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFrame, HomeFragment(), HomeFragmentTag).commit()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    private val backStackChangedListener = {
            val stackHeight = supportFragmentManager.backStackEntryCount
            if (stackHeight > 0) { // if we have something on the stack (doesn't include the current shown fragment)
                supportActionBar!!.setHomeButtonEnabled(true)
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            } else {
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setHomeButtonEnabled(false)
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(HomeFragmentTag)
        if (fragment != null && fragment.isVisible && fragment is HomeFragment) {
            when (item.itemId) {
                R.id.nav_settings -> handleSettingsTap()
                R.id.nav_calendar -> handleCalendarTap(fragment)
                R.id.nav_recheck -> handleRecheckTap(fragment)
            }
        } else if (item.itemId == android.R.id.home) {
            supportFragmentManager.popBackStack()
        }
        return true
    }

    private fun handleRecheckTap(fragment: HomeFragment) {
        if (TaskSchedulerHelper.canRecheck(this)) {
            fragment.getApod(TaskSchedulerHelper.getLatestDate(), true)
        } else {
            Toast.makeText(this, getString(R.string.checked_too_recently), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCalendarTap(fragment: HomeFragment) {
        val datePicker = DatePickerDialog(
            this, fragment.dateSetListener,
            fragment.selectedYear,
            fragment.selectedMonth - 1,
            fragment.selectedDay
        )
        val cal = Calendar.getInstance()
        datePicker.datePicker.minDate = cal.apply {
            set(Calendar.YEAR, 1995)
            set(Calendar.MONTH, 5)
            set(Calendar.DAY_OF_MONTH, 20)
        }.timeInMillis
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun handleSettingsTap() = supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(
            R.anim.enter_from_right,
            R.anim.exit_to_left,
            R.anim.enter_from_left,
            R.anim.exit_to_right
        )
        .replace(R.id.mainFrame, SettingsFragment(), "settings_fragment")
        .addToBackStack(null)
        .commit()
}
