package sk.pluk64.unibakonto;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import java.util.Map;

import sk.pluk64.unibakonto.http.JedalneListky;
import sk.pluk64.unibakonto.http.UnibaKonto;

public class TabbedActivity extends AppCompatActivity {
    static final String PREF_USERNAME = "username";
    static final String PREF_PASSWORD = "password";
    private static final String PREF_LOGGED_IN = "logged_in";
    private static final String PREF_SELECTED_PAGE = "selected_page";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private UnibaKonto unibaKonto = null;
    private boolean isLoggedIn = false;
    private ImageButton logoutButton;
    private Fragment curFragmentPos0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setCurrentItem(getPreferences(MODE_PRIVATE).getInt(PREF_SELECTED_PAGE, 0));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                getPreferences(MODE_PRIVATE).edit().putInt(PREF_SELECTED_PAGE, position).commit();
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        logoutButton = (ImageButton) findViewById(R.id.icon_logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsLoggedIn(false);
                // TODO hack
                if (curFragmentPos0 instanceof AccountFragment) {
                    findViewById(R.id.card_view).setVisibility(View.GONE);
                    findViewById(R.id.transactions_history).setVisibility(View.GONE);
                    ((AccountFragment) curFragmentPos0).swipeRefresh.setRefreshing(false);
                }
                if (curFragmentPos0 != null) {
                    removeFragment(curFragmentPos0);
                }
            }
        });

        setIsLoggedIn(getPrefLoggedIn());
    }

    void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        getPreferences(Context.MODE_PRIVATE).edit().putBoolean(PREF_LOGGED_IN, isLoggedIn).commit();

        enableLogoutButton(isLoggedIn);
    }

    void enableLogoutButton(boolean enabled) {
        logoutButton.setEnabled(enabled);
        if (enabled) {
            logoutButton.setImageResource(R.drawable.ic_logout_white_36dp);
        } else {
            logoutButton.setImageResource(R.drawable.ic_logout_grey600_36dp);
        }
    }

    private boolean getPrefLoggedIn() {
        return getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_LOGGED_IN, false);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (isLoggedIn) {
                    curFragmentPos0 = new AccountFragment();
                } else {
                    curFragmentPos0 = new LoginFragment();
                }
                return curFragmentPos0;
            } else if (position == 1) {
                return MenuFragment.newInstance(JedalneListky.Jedalne.VENZA);
            } else {
                return MenuFragment.newInstance(JedalneListky.Jedalne.EAM);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "E-peňaženka";
                case 1:
                    return "Venza";
                case 2:
                    return "Eat & Meet";
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof LoginFragment && isLoggedIn) {
                return POSITION_NONE;
            }
            if (object instanceof AccountFragment && !isLoggedIn) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    void removeFragment(Fragment oldFragment) {
        getSupportFragmentManager().beginTransaction()
                .remove(oldFragment)
                .commitAllowingStateLoss();
        mSectionsPagerAdapter.notifyDataSetChanged();
    }


    UnibaKonto getUnibaKonto() {
        if (unibaKonto == null) {
            UsernamePassword up = getUsernamePassword();
            unibaKonto = new UnibaKonto(up.username, up.password);
        }
        return unibaKonto;
    }

    public void invalidateUnibaKonto() {
        unibaKonto = null;
    }

    UsernamePassword getUsernamePassword() {
        return new UsernamePassword().invoke();
    }

    class UsernamePassword {
        String username;
        String password;

        public UsernamePassword invoke() {
            Map<String, ?> prefs = getPreferences(Context.MODE_PRIVATE).getAll();
            username = (String) prefs.get(PREF_USERNAME);
            password = (String) prefs.get(PREF_PASSWORD);
            return this;
        }
    }
}
