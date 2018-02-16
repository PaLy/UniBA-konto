package sk.pluk64.unibakonto.fragments;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.TabbedActivity;
import sk.pluk64.unibakonto.Utils;
import sk.pluk64.unibakonto.http.UnibaKonto;
import sk.pluk64.unibakonto.http.Util;

class UpdateAccountDataTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<TabbedActivity> activityReference;
    private final WeakReference<AccountFragment> fragmentReference;
    private boolean noInternet = false;
    private List<UnibaKonto.Transaction> updatedTransactions;
    private Map<String, UnibaKonto.Balance> balances;
    private List<UnibaKonto.CardInfo> cards;

    public UpdateAccountDataTask(TabbedActivity myActivity, AccountFragment accountFragment) {
        activityReference = new WeakReference<>(myActivity);
        fragmentReference = new WeakReference<>(accountFragment);
    }

    private void showNoInternetConnectionToastFromBackgroundThread() {
        final TabbedActivity activity = activityReference.get();
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
        TabbedActivity activity = activityReference.get();
        if (activity == null) {
            return false;
        } else {
            UnibaKonto unibaKonto = activity.getUnibaKonto();
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
            fragment.onUpdateTaskFinished(success, noInternet, balances, updatedTransactions, cards);
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
