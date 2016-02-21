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

import java.util.Map;

import sk.pluk64.unibakonto.http.JedalneListky;
import sk.pluk64.unibakonto.http.UnibaKonto;

public class TabbedActivity extends AppCompatActivity {
    static final String PREF_USERNAME = "username";
    static final String PREF_PASSWORD = "password";

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
    private boolean wasLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UsernamePassword up = getUsernamePassword();
        wasLoggedIn = up.username != null && up.password != null;

        setContentView(R.layout.activity_tabbed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
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
                if (wasLoggedIn) {
                    return new AccountFragment();
                } else {
                    return new LoginFragment();
                }
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
            if (object instanceof LoginFragment && wasLoggedIn) {
                return POSITION_NONE;
            }
            if (object instanceof AccountFragment && !wasLoggedIn) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    void replaceFragment(Fragment oldFragment) {
        wasLoggedIn = oldFragment instanceof LoginFragment;

        getSupportFragmentManager().beginTransaction()
                .remove(oldFragment)
                .commit();
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
