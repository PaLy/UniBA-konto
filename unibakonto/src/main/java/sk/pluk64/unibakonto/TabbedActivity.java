package sk.pluk64.unibakonto;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.http.UnibaKonto;
import sk.pluk64.unibakonto.meals.Menza;

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
    private SharedPreferences preferences;
    private ImageButton cardsButton;
    private boolean forceRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);
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
                preferences.edit().putInt(PREF_SELECTED_PAGE, position).apply();
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

        final DialogFragment cardsDialog = new CardsDialog();

        cardsButton = (ImageButton) findViewById(R.id.icon_card);
        cardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardsDialog.show(getSupportFragmentManager(), "cards");
            }
        });
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public static class CardsDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_card);
            builder.setPositiveButton(R.string.ok, null);

            builder.setMessage(getFormattedCardsData());
            return builder.create();
        }

        public SpannableStringBuilder getFormattedCardsData() {
            SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
            List<UnibaKonto.CardInfo> cardInfos = AccountFragment.loadCards(preferences, new Gson());
            SpannableStringBuilder resultBuilder = new SpannableStringBuilder();
            if (cardInfos != null) {
                for (UnibaKonto.CardInfo cardInfo : cardInfos) {
                    SpannableString ss = new SpannableString(cardInfo.number);
                    ss.setSpan(new RelativeSizeSpan(1.6f), 0, ss.length(), 0);

                    resultBuilder.append(ss);
                    resultBuilder.append(String.format("\n(%s: %s)\n\n", getString(R.string.valid_until), cardInfo.validUntil));
                }
            }
            if (resultBuilder.length() > 0) {
                resultBuilder.delete(resultBuilder.length() - 2, resultBuilder.length());
            } else {
                resultBuilder.append(getString(R.string.no_cards));
                resultBuilder.append("\n\n");
                resultBuilder.append(String.format("(%s)", getString(R.string.try_logout)));
            }
            return resultBuilder;
        }
    }

    private void deleteUserData() {
        getPreferences(MODE_PRIVATE).edit()
//                .remove(PREF_PASSWORD) // TODO think about it
                .remove(AccountFragment.PREF_BALANCES)
                .remove(AccountFragment.PREF_TRANSACTIONS)
                .remove(AccountFragment.PREF_ACCOUNT_REFRESH_TIMESTAMP)
                .apply();
    }

    void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        preferences.edit().putBoolean(PREF_LOGGED_IN, isLoggedIn).apply();

        setLogoutButtonEnabled(isLoggedIn);
    }

    void setLogoutButtonEnabled(boolean enabled) {
        logoutButton.setEnabled(enabled);
        if (enabled) {
            logoutButton.setImageResource(R.drawable.ic_logout_white_36dp);
        } else {
            logoutButton.setImageResource(R.drawable.ic_logout_grey600_36dp);
        }
    }

    void setCardsButtonEnabled(boolean enabled) {
        cardsButton.setEnabled(enabled);
        if (enabled) {
            cardsButton.setImageResource(R.drawable.ic_credit_card_white_36dp);
        } else {
            cardsButton.setImageResource(R.drawable.ic_credit_card_grey600_36dp);
        }
    }

    private boolean getPrefLoggedIn() {
        return getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_LOGGED_IN, false);
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
                return MenuFragment.newInstance(Menza.VENZA);
            } else {
                return MenuFragment.newInstance(Menza.EAM);
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
                .commitNow();
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
