package sk.pluk64.unibakontoapp.fragments;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.UnibaKonto;
import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.MainActivity;
import sk.pluk64.unibakontoapp.Utils;

class UpdateAccountDataTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<MainActivity> activityReference;
    private final WeakReference<AccountFragment> fragmentReference;
    private final WeakReference<TabbedFragment> tabbedFragmentReference;
    private boolean noInternet = false;
    private List<UnibaKonto.Transaction> updatedTransactions;
    private Map<String, UnibaKonto.Balance> balances;
    private String clientName;
    private List<UnibaKonto.CardInfo> cards;

    public UpdateAccountDataTask(MainActivity myActivity, AccountFragment accountFragment, TabbedFragment tabbedFragment) {
        activityReference = new WeakReference<>(myActivity);
        fragmentReference = new WeakReference<>(accountFragment);
        tabbedFragmentReference = new WeakReference<>(tabbedFragment);
    }

    private void showNoInternetConnectionToastFromBackgroundThread() {
        final MainActivity activity = activityReference.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.showNoInternetConnection(activity.getApplicationContext());
                }
            });
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        TabbedFragment tabbedFragment = tabbedFragmentReference.get();
        if (tabbedFragment == null) {
            return false;
        } else {
            UnibaKonto unibaKonto = tabbedFragment.getUnibaKonto();
            if (!unibaKonto.isLoggedIn(true)) {
                try {
                    unibaKonto.login();
                } catch (Util.ConnectionFailedException e) {
                    noInternet = true;
                    showNoInternetConnectionToastFromBackgroundThread();
                }
            }
            if (unibaKonto.isLoggedIn()) {
                try {
                    balances = unibaKonto.getBalances();
                    clientName = unibaKonto.getClientName();
                    updatedTransactions = unibaKonto.getTransactions();
                    cards = unibaKonto.getCards();
                    return true;
                } catch (Util.ConnectionFailedException e) {
                    noInternet = true;
                    showNoInternetConnectionToastFromBackgroundThread();
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        AccountFragment fragment = fragmentReference.get();
        if (fragment != null) {
            fragment.onUpdateTaskFinished(success, noInternet, balances, clientName, updatedTransactions, cards);
        }
    }

    @Override
    protected void onCancelled(Boolean success) {
        AccountFragment fragment = fragmentReference.get();
        if (fragment != null) {
            fragment.onUpdateTaskCancelled();
        }
    }
}
