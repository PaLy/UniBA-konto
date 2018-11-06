package sk.pluk64.unibakontoapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.Map;

import sk.pluk64.unibakonto.UnibaKonto;
import sk.pluk64.unibakontoapp.MainActivity;
import sk.pluk64.unibakontoapp.R;
import sk.pluk64.unibakontoapp.UpdateMenusListener;
import sk.pluk64.unibakontoapp.fragments.menu.MenuFragment;
import sk.pluk64.unibakontoapp.meals.Menza;

import static android.content.Context.MODE_PRIVATE;

public class TabbedFragment extends Fragment implements UpdateMenusListener {
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
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
    private Fragment curFragmentPos0;
    private SharedPreferences preferences;
    private boolean forceRefresh = false;
    private MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
            preferences = activity.getPreferences(MODE_PRIVATE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_tabbed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = view.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setCurrentItem(preferences.getInt(PREF_SELECTED_PAGE, 0));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                preferences.edit().putInt(PREF_SELECTED_PAGE, position).apply();
                final InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    @Override
    public void updateMenus() {
        for (int i = 1; i < 3; i++) {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(
                    "android:switcher:" + mViewPager.getId() + ":" + mSectionsPagerAdapter.getItemId(i)
            );
            if (fragment instanceof MenuFragment) {
                ((MenuFragment) fragment).updateData();
            }
        }
    }

    private void deleteUserData() {
        preferences.edit()
//                .remove(PREF_PASSWORD) // TODO think about it
                .remove(AccountFragment.PREF_BALANCES)
                .remove(AccountFragment.PREF_TRANSACTIONS)
                .remove(AccountFragment.PREF_ACCOUNT_REFRESH_TIMESTAMP)
                .apply();
    }

    public void saveLoginDetails(String username, String password) {
        String previousUsername = preferences.getString(PREF_USERNAME, null);
        if (!username.equals(previousUsername)) {
            deleteUserData();
        }
        preferences.edit()
                .putString(PREF_USERNAME, username)
                .putString(PREF_PASSWORD, password)
                .apply();
    }

    private boolean isLoggedIn() {
        return activity.isLoggedIn();
    }

    public void onLogout() {
        // TODO hack
        if (curFragmentPos0 instanceof AccountFragment) {
            View view = getView();
            if (view != null) {
                view.findViewById(R.id.balances_card_view).setVisibility(View.GONE);
                view.findViewById(R.id.transactions_history).setVisibility(View.GONE);
            }
            ((AccountFragment) curFragmentPos0).getSwipeRefresh().setRefreshing(false);
        }
        if (curFragmentPos0 != null) {
            removeFragment(curFragmentPos0);
        }
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
                if (isLoggedIn() && !(curFragmentPos0 instanceof AccountFragment)) {
                    curFragmentPos0 = new AccountFragment();
                }
                if (!isLoggedIn() && !(curFragmentPos0 instanceof LoginFragment)){
                    curFragmentPos0 = new LoginFragment();
                }
                return curFragmentPos0;
            } else if (position == 1) {
                return MenuFragment.newInstance(Menza.VENZA, TabbedFragment.this);
            } else {
                return MenuFragment.newInstance(Menza.EAM, TabbedFragment.this);
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
                    return getString(R.string.title_e_wallet);
                case 1:
                    return "Venza";
                case 2:
                    return "Eat & Meet";
            }
            return null;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            if (object instanceof LoginFragment && isLoggedIn()) {
                return POSITION_NONE;
            }
            if (object instanceof AccountFragment && !isLoggedIn()) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    public void removeFragment(Fragment oldFragment) {
        getChildFragmentManager().beginTransaction()
                .remove(oldFragment)
                .commitNow();
        mSectionsPagerAdapter.notifyDataSetChanged();
    }


    public UnibaKonto getUnibaKonto() {
        if (unibaKonto == null) {
            UsernamePassword up = getUsernamePassword();
            unibaKonto = new UnibaKonto(up.username, up.password);
        }
        return unibaKonto;
    }

    public void invalidateUnibaKonto() {
        unibaKonto = null;
    }

    public UsernamePassword getUsernamePassword() {
        return new UsernamePassword().invoke();
    }

    public class UsernamePassword {
        public String username;
        public String password;

        public UsernamePassword invoke() {
            Map<String, ?> prefs = preferences.getAll();
            username = (String) prefs.get(PREF_USERNAME);
            password = (String) prefs.get(PREF_PASSWORD);
            return this;
        }
    }
}
