package uk.co.jakelee.apodwallpaper

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uk.co.jakelee.apodwallpaper.fragments.HomeFragment
import uk.co.jakelee.apodwallpaper.fragments.SettingsFragment
import uk.co.jakelee.apodwallpaper.helper.PreferenceHelper
import uk.co.jakelee.apodwallpaper.scheduling.EndpointCheckScheduler
import uk.co.jakelee.apodwallpaper.scheduling.EndpointCheckTimingHelper
import java.util.*

class MainActivity : AppCompatActivity() {

    private val HomeFragmentTag = "HOME_FRAGMENT"

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefHelper = PreferenceHelper(this)
        if (shouldPerformSetup(prefHelper)) {
            EndpointCheckScheduler(this).scheduleJob()
            prefHelper.setBooleanPref(PreferenceHelper.BooleanPref.first_time_setup, true)
        }
        requestNotificationPermissionIfNeeded(prefHelper)
        keepContentBelowActionBar()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFrame, HomeFragment(), HomeFragmentTag).commit()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    /**
     * Targeting 35+ draws edge to edge, which pushes the action bar down by the status bar height
     * but leaves the content view where it was, so the action bar covers the top of every screen -
     * on the home screen that is the whole title. Padding the content down by the same inset lines
     * it back up underneath.
     */
    private fun keepContentBelowActionBar() {
        val content = findViewById<View>(R.id.mainFrame)
        // Read the window's own insets rather than listening on this view: the decor has already
        // consumed the top inset by the time it reaches here, so a listener would only ever see 0.
        content.post {
            val insets = ViewCompat.getRootWindowInsets(window.decorView) ?: return@post
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            content.setPadding(bars.left, bars.top, bars.right, content.paddingBottom)
        }
    }

    /**
     * From API 33 a notification is silently dropped without this permission - notify() does not
     * throw, it simply does nothing - so the daily update would appear to work while never
     * notifying. Only worth asking when the user actually wants notifications.
     */
    private fun requestNotificationPermissionIfNeeded(prefHelper: PreferenceHelper) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (!prefHelper.getBooleanPref(PreferenceHelper.BooleanPref.notifications_enabled)) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            fragment.getContent(EndpointCheckTimingHelper.getLatestDate(), true, true, item)
        } else {
            val recheck = EndpointCheckTimingHelper.getNextRecheckTime(this)
            val recheckTime =
                DateUtils.getRelativeTimeSpanString(recheck, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
            val recheckText =
                String.format(getString(R.string.checked_too_recently), recheckTime.toString().lowercase())
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
