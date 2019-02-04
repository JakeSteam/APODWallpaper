package uk.co.jakelee.apodwallpaper

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import uk.co.jakelee.apodwallpaper.fragments.HomeFragment
import uk.co.jakelee.apodwallpaper.fragments.SettingsFragment
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.scheduling.EndpointCheckScheduler
import uk.co.jakelee.apodwallpaper.scheduling.EndpointCheckTimingHelper
import java.util.*

class MainActivity : AppCompatActivity() {

    private val HomeFragmentTag = "HOME_FRAGMENT"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }
        val prefHelper = PreferenceHelper(this)
        if (shouldPerformSetup(prefHelper)) {
            EndpointCheckScheduler(this).scheduleJob()
            prefHelper.setBooleanPref(PreferenceHelper.BooleanPref.first_time_setup, true)
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFrame, HomeFragment(), HomeFragmentTag).commit()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    private fun shouldPerformSetup(prefHelper: PreferenceHelper) =
        (!prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.first_time_setup)
                && prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.automatic_enabled))

    private val backStackChangedListener = {
        val stackHeight = supportFragmentManager.backStackEntryCount
        if (stackHeight > 0) { // if we have something on the stack (doesn't include the current shown fragment)
            supportActionBar!!.setHomeButtonEnabled(true)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } else {
            supportActionBar!!.title = getString(R.string.app_name)
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
        if (item.itemId == android.R.id.home) {
            supportFragmentManager.popBackStack()
            supportActionBar!!.title = getString(R.string.app_name)
        } else if (fragment != null && fragment.isVisible && fragment is HomeFragment) {
            when (item.itemId) {
                R.id.nav_settings -> handleSettingsTap()
                R.id.nav_calendar -> handleCalendarTap(fragment)
                R.id.nav_recheck -> handleRecheckTap(item, fragment)
            }
        }
        return true
    }

    private fun handleRecheckTap(item: MenuItem, fragment: HomeFragment) {
        if (EndpointCheckTimingHelper.canRecheck(this)) {
            fragment.getApod(EndpointCheckTimingHelper.getLatestDate(), true, true, item)
        } else {
            val recheck = EndpointCheckTimingHelper.getNextRecheckTime(this)
            val recheckTime =
                DateUtils.getRelativeTimeSpanString(recheck, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
            val recheckText =
                String.format(getString(R.string.checked_too_recently), recheckTime.toString().toLowerCase())
            Toast.makeText(this, recheckText, Toast.LENGTH_SHORT).show()
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
            set(Calendar.MONTH, 6)
            set(Calendar.DAY_OF_MONTH, 16)
        }.timeInMillis
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        //datePicker.updateDate(fragment.selectedYear, fragment.selectedMonth, fragment.selectedDay)
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
        .add(R.id.mainFrame, SettingsFragment(), null)
        .addToBackStack(null)
        .commit()
}
