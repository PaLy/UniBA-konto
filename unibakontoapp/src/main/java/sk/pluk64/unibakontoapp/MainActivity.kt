package sk.pluk64.unibakontoapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.facebook.appevents.AppEventsLogger
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import sk.pluk64.unibakonto.IsUnibaKonto
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakontoapp.fragments.CardsDialog
import sk.pluk64.unibakontoapp.fragments.LoginFragment
import sk.pluk64.unibakontoapp.fragments.statistics.StatisticsFragment
import sk.pluk64.unibakontoapp.fragments.EwalletAndMenusFragment
import sk.pluk64.unibakontoapp.fragments.SetThemeDialog

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, RefreshClientDataUiListener {

    private val preferences by lazy { getPreferences(Context.MODE_PRIVATE) }

    var isLoggedIn = false
        set(isLoggedIn) {
            field = isLoggedIn
            preferences.edit().putBoolean(PreferencesKeys.LOGGED_IN, isLoggedIn).apply()

            setLogoutButtonEnabled(isLoggedIn)
            refreshClientDataUI()
        }
    private lateinit var logoutSection: MenuItem

    private val prefLoggedIn: Boolean
        get() = preferences.getBoolean(PreferencesKeys.LOGGED_IN, false)

    var unibaKonto: IsUnibaKonto = UnibaKonto.EMPTY
        get() {
            return if (field == UnibaKonto.EMPTY) {
                createUnibaKonto()
            } else {
                field
            }
        }
        private set

    private var onDrawerClosed = {}

    private fun createUnibaKonto(): UnibaKonto {
        val credentials = EwalletAndMenusFragment.Credentials(preferences)
        return UnibaKonto(credentials.username, credentials.password)
    }

    fun invalidateUnibaKonto() {
        unibaKonto = createUnibaKonto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            preferences.getInt(PreferencesKeys.THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
        super.onCreate(savedInstanceState)
        checkFirstRun()

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                this@MainActivity.onDrawerClosed()
                onDrawerClosed = {}
            }
        })
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        logoutSection = nav_view.menu.findItem(R.id.logout_section)
        isLoggedIn = prefLoggedIn

        refreshClientDataUI()

        AppEventsLogger.activateApp(application)

        unibaKonto = createUnibaKonto()

        showLastOpenedFragment()
    }

    private fun checkFirstRun() {
        val notExist = -1

        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = preferences.getInt(PreferencesKeys.VERSION_CODE, notExist)

        val saveVersion = { preferences.edit().putInt(PreferencesKeys.VERSION_CODE, currentVersionCode).apply() }

        when {
            // normal run
            currentVersionCode == savedVersionCode -> return
            // new install / user cleared shared preferences / updated from version < 36 (Gson removal)
            savedVersionCode == notExist -> {
                preferences.edit().clear().apply()
                saveVersion()
            }
            // update
            currentVersionCode > savedVersionCode -> saveVersion()
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_show_card) {
            CardsDialog().show(supportFragmentManager, "cards")
            return true
        }
        if (id == R.id.action_menu_refresh) {
            refreshVisibleFragments()
            return true
        }
        if (id == R.id.action_menu_set_theme) {
            SetThemeDialog().show(supportFragmentManager, "setTheme")
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isRefreshableFragmentVisible = supportFragmentManager.fragments.any {
            it.isVisible
                && it is Refreshable
                && it.canRefresh()
        }

        menu.findItem(R.id.action_menu_refresh).isVisible = isRefreshableFragmentVisible
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var showSelected = true
        // Handle navigation view item clicks here.
        val id = item.itemId

        onDrawerClosed = {
            when (id) {
                R.id.nav_ewallet -> showEwalletAndMenus()
                R.id.nav_statistics -> showStatisticsFragment()
                R.id.nav_logout -> {
                    isLoggedIn = false
                    ifTabbedFragmentVisible { it.onLogout() }
                    showSelected = false
                }
                R.id.nav_fb_eam -> {
                    openLink("https://www.facebook.com/eatandmeetmlyny/")
                    showSelected = false
                }
                R.id.nav_ig_eam -> {
                    openLink("https://www.instagram.com/eatandmeetmlyny/")
                    showSelected = false
                }
                R.id.nav_fb_venza -> {
                    openLink("https://www.facebook.com/venza.mlyny/")
                    showSelected = false
                }
                R.id.nav_ig_venza -> {
                    openLink("https://www.instagram.com/venza.na.mlynoch/")
                    showSelected = false
                }
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return showSelected
    }

    private fun openLink(uri: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(browserIntent)
    }

    private fun refreshVisibleFragments() {
        supportFragmentManager.fragments
            .asSequence()
            .filter { it.isVisible }
            .filterIsInstance<Refreshable>()
            .forEach { it.refresh() }
    }

    private fun ifTabbedFragmentVisible(action: (EwalletAndMenusFragment) -> Unit) {
        val tabbedFragment = supportFragmentManager.findFragmentByTag(TAG_TABBED_FRAGMENT) as EwalletAndMenusFragment?
        if (tabbedFragment != null && tabbedFragment.isVisible) {
            action.invoke(tabbedFragment)
        }
    }

    private fun showStatisticsFragment() {
        preferences.edit().putString(PreferencesKeys.SELECTED_MAIN_FRAGMENT, TAG_STATISTICS_FRAGMENT).apply()

        supportActionBar?.title = getString(R.string.my_statistics)

        val showStatistics = {
            val statisticsFragment = StatisticsFragment()
            replaceFragment(statisticsFragment, TAG_STATISTICS_FRAGMENT)
        }

        if (this.isLoggedIn) {
            showStatistics()
        } else {
            val loginFragment = LoginFragment()
            loginFragment.onSuccess = showStatistics
            replaceFragment(loginFragment, TAG_STATISTICS_LOGIN_FRAGMENT)
        }
    }

    private fun showLastOpenedFragment() {
        val lastOpened = preferences.getString(PreferencesKeys.SELECTED_MAIN_FRAGMENT, TAG_TABBED_FRAGMENT)
        when (lastOpened) {
            TAG_TABBED_FRAGMENT -> {
                nav_view.setCheckedItem(R.id.nav_ewallet)
                showEwalletAndMenus()
            }
            TAG_STATISTICS_FRAGMENT -> {
                nav_view.setCheckedItem(R.id.nav_statistics)
                showStatisticsFragment()
            }
        }
    }

    private fun showEwalletAndMenus() {
        preferences.edit().putString(PreferencesKeys.SELECTED_MAIN_FRAGMENT, TAG_TABBED_FRAGMENT).apply()

        supportActionBar?.title = getString(R.string.e_wallet_menus)

        val ewalletAndMenus = EwalletAndMenusFragment()
        ewalletAndMenus.setRefreshClientDataUiListener(this)
        replaceFragment(ewalletAndMenus, TAG_TABBED_FRAGMENT)
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.app_content, fragment, tag)
        fragmentTransaction.commit()

        invalidateOptionsMenu()
    }

    fun setLogoutButtonEnabled(enabled: Boolean) {
        logoutSection.isVisible = enabled
        logoutSection.isEnabled = enabled
    }

    override fun refreshClientDataUI() {
        val clientName = if (!prefLoggedIn) {
            ""
        } else {
            preferences.getString(PreferencesKeys.CLIENT_NAME, "") ?: ""
        }

        val clientNameView = nav_view.getHeaderView(0).findViewById<TextView>(R.id.client_name)
        if (clientNameView != null) {
            clientNameView.text = clientName
        }
    }


    private fun deleteUserData() {
        preferences.edit()
            // .remove(PreferencesKeys.PASSWORD) // TODO think about it
            .remove(PreferencesKeys.BALANCES)
            .remove(PreferencesKeys.CLIENT_NAME)
            .remove(PreferencesKeys.TRANSACTIONS)
            .remove(PreferencesKeys.ALL_TRANSACTIONS)
            .remove(PreferencesKeys.ACCOUNT_REFRESH_TIMESTAMP)
            .remove(PreferencesKeys.TRANSACTIONS_REFRESH_TIMESTAMP)
            .remove(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP)
            .apply()
    }

    fun saveLoginDetails(username: String, password: String) {
        val previousUsername = preferences.getString(PreferencesKeys.USERNAME, null)
        if (username != previousUsername) {
            deleteUserData()
        }
        preferences.edit()
            .putString(PreferencesKeys.USERNAME, username)
            .putString(PreferencesKeys.PASSWORD, password)
            .apply()
    }

    companion object {
        private const val TAG_TABBED_FRAGMENT = "TABBED_FRAGMENT"
        private const val TAG_STATISTICS_FRAGMENT = "STATISTICS_FRAGMENT"
        private const val TAG_STATISTICS_LOGIN_FRAGMENT = "STATISTICS_LOGIN_FRAGMENT"
    }
}
