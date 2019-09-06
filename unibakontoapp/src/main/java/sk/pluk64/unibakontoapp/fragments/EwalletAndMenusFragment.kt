package sk.pluk64.unibakontoapp.fragments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_tabbed.view.*
import kotlinx.android.synthetic.main.fragment_account.view.*
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.fragments.menu.MenuFragment
import sk.pluk64.unibakontoapp.meals.Canteen

class EwalletAndMenusFragment : Fragment(), RefreshMenusListener, Refreshable {

    private lateinit var activity: MainActivity
    private val preferences by lazy { activity.getPreferences(MODE_PRIVATE) }
    private lateinit var mView: View

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    private var curFragmentPos0: Fragment? = null
    var isForceRefresh = false
    private var refreshClientDataUiListener: RefreshClientDataUiListener? = null

    private val currentFragment: Fragment?
        get() = getFragmentByPagerItemId(mView.viewPager.currentItem.toLong())

    private val isLoggedIn: Boolean
        get() = activity.isLoggedIn

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_tabbed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mView = view
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(childFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mView.viewPager.adapter = mSectionsPagerAdapter
        mView.viewPager.offscreenPageLimit = 2
        mView.viewPager.currentItem = preferences.getInt(PreferencesKeys.SELECTED_PAGE_TABBED_FRAGMENT, 0)
        mView.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                preferences.edit().putInt(PreferencesKeys.SELECTED_PAGE_TABBED_FRAGMENT, position).apply()
                val imm = activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mView.viewPager.windowToken, 0)

                activity.invalidateOptionsMenu()
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        val tabLayout = view.findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(mView.viewPager)
    }

    override fun refreshMenus() {
        for (i in 0 until CHILD_FRAGMENTS_COUNT) {
            val fragment = getFragmentByPosition(i)

            if (fragment is MenuFragment) {
                fragment.refresh()
            }
        }
    }

    private fun getFragmentByPosition(i: Int): Fragment? {
        return getFragmentByPagerItemId(mSectionsPagerAdapter.getItemId(i))
    }

    private fun getFragmentByPagerItemId(itemId: Long): Fragment? {
        return childFragmentManager.findFragmentByTag(
            "android:switcher:" + mView.viewPager.id + ":" + itemId
        )
    }

    private fun refreshCurrentFragment() {
        val fragment = currentFragment

        if (fragment is Refreshable) {
            (fragment as Refreshable).refresh()
        }
    }

    override fun refresh() {
        refreshCurrentFragment()
    }


    override fun canRefresh(): Boolean {
        val currentFragment = currentFragment
        return currentFragment is Refreshable && (currentFragment as Refreshable).canRefresh()
    }

    fun onLogout() {
        // TODO hack
        if (curFragmentPos0 is AccountFragment) {
            val view = view
            if (view != null) {
                view.balances_card_view.visibility = View.GONE
                view.transactions_history.visibility = View.GONE
            }
            (curFragmentPos0 as AccountFragment).swipeRefresh.isRefreshing = false
        }
        if (curFragmentPos0 != null) {
            removeFragment(curFragmentPos0!!)
        }
    }

    fun setRefreshClientDataUiListener(refreshClientDataUiListener: RefreshClientDataUiListener) {
        this.refreshClientDataUiListener = refreshClientDataUiListener
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            if (position == 0) {
                if (isLoggedIn && curFragmentPos0 !is AccountFragment) {
                    curFragmentPos0 = AccountFragment()
                    refreshClientDataUiListener?.let {
                        (curFragmentPos0 as AccountFragment).setRefreshClientDataUiListener(it)
                    }
                }
                if (!isLoggedIn && curFragmentPos0 !is LoginFragment) {
                    val loginFragment = LoginFragment()
                    loginFragment.onSuccess = {
                        isForceRefresh = true
                        removeFragment(loginFragment)
                    }
                    curFragmentPos0 = loginFragment
                }
                return curFragmentPos0!!
            } else return if (position == 1) {
                MenuFragment.newInstance(Canteen.VENZA, this@EwalletAndMenusFragment)
            } else {
                MenuFragment.newInstance(Canteen.EAM, this@EwalletAndMenusFragment)
            }
        }

        override fun getCount(): Int {
            return CHILD_FRAGMENTS_COUNT
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return getString(R.string.title_e_wallet)
                1 -> return "Venza"
                2 -> return "Eat & Meet"
            }
            return null
        }

        override fun getItemPosition(fragment: Any): Int {
            if (fragment is LoginFragment && isLoggedIn) {
                return PagerAdapter.POSITION_NONE
            }
            return if (fragment is AccountFragment && !isLoggedIn) {
                PagerAdapter.POSITION_NONE
            } else PagerAdapter.POSITION_UNCHANGED
        }
    }

    fun removeFragment(oldFragment: Fragment) {
        if (oldFragment.isAdded) {
            childFragmentManager.beginTransaction()
                .remove(oldFragment)
                .commitNow()
        }
        if (isAdded) {
            mSectionsPagerAdapter.notifyDataSetChanged()
        }

        activity.invalidateOptionsMenu()
    }

    class Credentials(preferences: SharedPreferences) {
        val username = preferences.getString(PreferencesKeys.USERNAME, "") ?: ""
        val password = preferences.getString(PreferencesKeys.PASSWORD, "") ?: ""
    }

    companion object {
        private const val CHILD_FRAGMENTS_COUNT = 3
    }
}
