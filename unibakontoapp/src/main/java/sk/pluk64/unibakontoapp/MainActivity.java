package sk.pluk64.unibakontoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Consumer;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;

import java9.util.stream.StreamSupport;
import sk.pluk64.unibakontoapp.fragments.AccountFragment;
import sk.pluk64.unibakontoapp.fragments.CardsDialog;
import sk.pluk64.unibakontoapp.fragments.TabbedFragment;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener, RefreshClientDataUiListener {
    private static final String PREF_LOGGED_IN = "logged_in";
    private static final String TAG_TABBED_FRAGMENT = "TABBED_FRAGMENT";

    private SharedPreferences preferences;
    private boolean isLoggedIn = false;
    private MenuItem logoutSection;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_ewallet);
        showTabbedFragment();

        logoutSection = navigationView.getMenu().findItem(R.id.logout_section);
        setIsLoggedIn(getPrefLoggedIn());

        refreshClientDataUI();

        AppEventsLogger.activateApp(getApplication());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_show_card) {
            new CardsDialog().show(getSupportFragmentManager(), "cards");
            return true;
        }
        if (id == R.id.action_menu_refresh) {
            refreshVisibleFragments();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isRefreshableFragmentVisible =
            StreamSupport.stream(getSupportFragmentManager().getFragments())
                .anyMatch(fragment ->
                    fragment.isVisible()
                        && fragment instanceof Refreshable
                        && ((Refreshable) fragment).canRefresh()
                );

        menu.findItem(R.id.action_menu_refresh).setVisible(isRefreshableFragmentVisible);
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        boolean showSelected = true;
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_ewallet) {
            showTabbedFragment();
        } else if (id == R.id.nav_statistics) {
        } else if (id == R.id.nav_logout) {
            setIsLoggedIn(false);
            ifTabbedFragmentVisible(TabbedFragment::onLogout);
            showSelected = false;
        } else if (id == R.id.nav_fb_eam) {
            openLink("https://www.facebook.com/eatandmeetmlyny/");
            showSelected = false;
        } else if (id == R.id.nav_ig_eam) {
            openLink("https://www.instagram.com/eatandmeetmlyny/");
            showSelected = false;
        } else if (id == R.id.nav_fb_venza) {
            openLink("https://www.facebook.com/venza.mlyny/");
            showSelected = false;
        } else if (id == R.id.nav_ig_venza) {
            openLink("https://www.instagram.com/venza.na.mlynoch/");
            showSelected = false;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return showSelected;
    }

    private void openLink(String uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(browserIntent);
    }

    private void refreshVisibleFragments() {
        StreamSupport.stream(getSupportFragmentManager().getFragments())
            .filter(Fragment::isVisible)
            .filter(Refreshable.class::isInstance)
            .map(fragment -> (Refreshable) fragment)
            .forEach(Refreshable::refresh);
    }

    private void ifTabbedFragmentVisible(Consumer<TabbedFragment> action) {
        TabbedFragment tabbedFragment = (TabbedFragment) getSupportFragmentManager().findFragmentByTag(TAG_TABBED_FRAGMENT);
        if (tabbedFragment != null && tabbedFragment.isVisible()) {
            action.accept(tabbedFragment);
        }
    }

    private void showTabbedFragment() {
        TabbedFragment tabbedFragment = new TabbedFragment();
        tabbedFragment.setRefreshClientDataUiListener(this);
        replaceFragment(tabbedFragment, TAG_TABBED_FRAGMENT);
    }

    private void replaceFragment(TabbedFragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.app_content, fragment, tag);
        fragmentTransaction.commit();

        invalidateOptionsMenu();
    }

    public boolean isLoggedIn() {
        return this.isLoggedIn;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        preferences.edit().putBoolean(PREF_LOGGED_IN, isLoggedIn).apply();

        setLogoutButtonEnabled(isLoggedIn);
        refreshClientDataUI();
    }

    public void setLogoutButtonEnabled(boolean enabled) {
        logoutSection.setVisible(enabled);
        logoutSection.setEnabled(enabled);
    }

    private boolean getPrefLoggedIn() {
        return preferences.getBoolean(PREF_LOGGED_IN, false);
    }

    @Override
    public void refreshClientDataUI() {
        String clientName;
        if (!getPrefLoggedIn()) {
            clientName = "";
        } else {
            clientName = preferences.getString(AccountFragment.PREF_CLIENT_NAME, "");
        }

        TextView clientNameView = navigationView.getHeaderView(0).findViewById(R.id.client_name);
        if (clientNameView != null) {
            clientNameView.setText(clientName);
        }
    }
}
