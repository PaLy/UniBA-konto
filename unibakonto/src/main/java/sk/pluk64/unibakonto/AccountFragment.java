package sk.pluk64.unibakonto;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.http.UnibaKonto;
import sk.pluk64.unibakonto.http.Util;

public class AccountFragment extends Fragment {
    static final String PREF_BALANCES = "balances";
    static final String PREF_TRANSACTIONS = "transactions";
//    static final String PREF_ACCOUNT_REFRESH_TIMESTAMP = "account_refresh_timestamp"; // DO NOT USE - could contain old data (string)
    static final String PREF_ACCOUNT_REFRESH_TIMESTAMP = "account_refresh_timestamp_date";
    static final String PREF_CARDS = "cards";
    private Map<String, UnibaKonto.Balance> balances = Collections.emptyMap();
    private MyAdapter mAdapter = new MyAdapter();
    SwipeRefreshLayout swipeRefresh;
    private SharedPreferences preferences;
    private AsyncTask<Void, Void, Boolean> updateDataTask;
    private List<UnibaKonto.CardInfo> cards;
    private Date refreshTime;

    public AccountFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    private void updateData() {
        if (updateDataTask != null) {
            return;
        }
        View view = getView();
        if (view != null) {
            setRefreshing(view);
        }
        TabbedActivity activity = getMyActivity();
        if (activity != null) {
            activity.setLogoutButtonEnabled(false);
            activity.setCardsButtonEnabled(false);
            activity.setForceRefresh(false);
        }

        updateDataTask = new AsyncTask<Void, Void, Boolean>() {
            public boolean noInternet = false;
            private List<UnibaKonto.Transaction> updatedTransactions;

            @Override
            protected Boolean doInBackground(Void... params) {
                TabbedActivity activity = getMyActivity();
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
                TabbedActivity activity = getMyActivity();
                View view = getView();

                if (activity != null) {
                    activity.setCardsButtonEnabled(true);
                }

                if (success) {
                    saveData(balances, updatedTransactions, cards);
                    if (view != null) {
                        updateViewBalances(view);
                    }
                    MyAdapter adapter = AccountFragment.this.mAdapter;
                    adapter.getData().clear();
                    adapter.getData().addAll(updatedTransactions);
                    adapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                    if (activity != null) {
                        activity.setLogoutButtonEnabled(true);
                    }
                } else if (!noInternet) {
                    if (activity != null) {
                        if (view != null) {
                            // from whatever reason card view and swipeRefresh stay on screen after fragment replacement
                            // this workaround will hide it
                            // TODO could potentially leaks memory?
                            view.findViewById(R.id.card_view).setVisibility(View.GONE);
                            view.findViewById(R.id.transactions_history).setVisibility(View.GONE);
                            swipeRefresh.setRefreshing(false);
                        }

                        activity.setIsLoggedIn(false);
                        activity.removeFragment(AccountFragment.this);
                    }
                } else {
                    if (activity != null) {
                        activity.setLogoutButtonEnabled(true);
                    }
                    swipeRefresh.setRefreshing(false);
                }
                if (view != null) {
                    updateRefreshTime(view);
                }
                updateDataTask = null;
            }
        };
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void saveData(Map<String, UnibaKonto.Balance> balances, List<UnibaKonto.Transaction> transactions, List<UnibaKonto.CardInfo> cards) {
        Gson gson = new Gson();
        String jsonBalances = gson.toJson(balances);
        String jsonTransactions = gson.toJson(transactions);
        String jsonCards = gson.toJson(cards);
        refreshTime = Utils.getCurrentTime();
        String jsonRefreshTime = gson.toJson(refreshTime);
        preferences.edit()
                .putString(PREF_BALANCES, jsonBalances)
                .putString(PREF_TRANSACTIONS, jsonTransactions)
                .putString(PREF_CARDS, jsonCards)
                .putString(PREF_ACCOUNT_REFRESH_TIMESTAMP, jsonRefreshTime)
                .apply();
    }

    private void loadData() {
        Gson gson = new Gson();

        String jsonBalances = preferences.getString(PREF_BALANCES, "null");
        balances = gson.fromJson(jsonBalances, new TypeToken<Map<String, UnibaKonto.Balance>>(){}.getType());
        if (balances == null) {
            balances = Collections.emptyMap();
        }

        String jsonTransactions = preferences.getString(PREF_TRANSACTIONS, "null");
        List<UnibaKonto.Transaction> transactions =
                gson.fromJson(jsonTransactions, new TypeToken<List<UnibaKonto.Transaction>>(){}.getType());
        if (transactions != null) {
            mAdapter.getData().clear();
            mAdapter.getData().addAll(transactions);
            mAdapter.notifyDataSetChanged();
        }

        cards = loadCards(preferences, gson);

        String jsonRefreshTime = preferences.getString(PREF_ACCOUNT_REFRESH_TIMESTAMP, "null");
        refreshTime = gson.fromJson(jsonRefreshTime, new TypeToken<Date>(){}.getType());
    }

    static List<UnibaKonto.CardInfo> loadCards(SharedPreferences preferences, Gson gson) {
        String jsonCards = preferences.getString(PREF_CARDS, "null");
        return gson.fromJson(jsonCards, new TypeToken<List<UnibaKonto.CardInfo>>(){}.getType());
    }

    private void setRefreshing(View view) {
        TextView timestamp = (TextView) view.findViewById(R.id.refresh_timestamp);
        timestamp.setText(getString(R.string.refreshing));
    }

    private void updateRefreshTime(View view) {
        TextView timestamp = (TextView) view.findViewById(R.id.refresh_timestamp);
        String refreshTimeFormatted = Utils.getTimeFormatted(refreshTime, getString(R.string.never));
        timestamp.setText(getString(R.string.refreshed, refreshTimeFormatted));
    }

    private void showNoInternetConnectionToastFromBackgroundThread() {
        getMyActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.showNoInternetConnection(getMyActivity().getApplicationContext());
            }
        });
    }

    private TabbedActivity getMyActivity() {
        return ((TabbedActivity) getActivity());
    }

    private void updateViewBalances(View view) {
        Object[][] viewIdBalanceId = {
                {R.id.text_balance, UnibaKonto.ID_ACCOUNT},
                {R.id.text_deposit, UnibaKonto.ID_DEPOSIT},
                {R.id.text_deposit2, UnibaKonto.ID_DEPOSIT2},
                {R.id.text_zaloha, UnibaKonto.ID_ZALOHA}
        };
        for (Object[] vb : viewIdBalanceId) {
            UnibaKonto.Balance data = balances.get(vb[1]);
            TextView textView = (TextView) view.findViewById((Integer) vb[0]);
            if (data != null) {
                textView.setText(Html.fromHtml("<b>" + data.label + "</b>" + " " + data.price));
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setText("");
                textView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        RecyclerView transactionsView = (RecyclerView) view.findViewById(R.id.transactions_history);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        transactionsView.setHasFixedSize(true);
        RecyclerView.LayoutManager tLayoutManager = new LinearLayoutManager(getContext());
        transactionsView.setLayoutManager(tLayoutManager);
        transactionsView.setAdapter(mAdapter);

        loadData();
        updateRefreshTime(view);

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
        updateViewBalances(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isTimeDiffMoreThanXHours(refreshTime, 2) ||
                getMyActivity() != null && getMyActivity().isForceRefresh()) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    updateData();
                }
            });
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<UnibaKonto.Transaction> transactions = new ArrayList<>();

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            public CardView view;

            public ViewHolder(CardView v) {
                super(v);
                view = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter() {
        }

        public List<UnibaKonto.Transaction> getData() {
            return transactions;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.transaction, parent, false);
            return new ViewHolder((CardView) v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UnibaKonto.Transaction transaction = transactions.get(position);
            CardView transactionsView = holder.view;
            Context context = transactionsView.getContext();

            TableLayout transactionTable = (TableLayout) transactionsView.findViewById(R.id.transaction_table);
            transactionTable.removeAllViews();
            for (UnibaKonto.TransactionItem transactionItem : transaction.transactionItems) {
                TableRow transactionItemView = (TableRow) LayoutInflater.from(context)
                        .inflate(R.layout.transaction_item, transactionTable, false);

                TextView descriptionView = (TextView) transactionItemView.findViewById(R.id.transaction_item_description);
                descriptionView.setText(transactionItem.description);

                TextView amountView = (TextView) transactionItemView.findViewById(R.id.transaction_item_price);
                amountView.setText(String.format("%+,.2f€", transactionItem.parsedAmount));

                transactionTable.addView(transactionItemView);
            }

            TextView timestampView = (TextView) transactionsView.findViewById(R.id.transaction_timestamp);
            timestampView.setText(transaction.getTimestamp());

            TextView totalAmountView = (TextView) transactionsView.findViewById(R.id.transaction_total);
            double totalAmount = transaction.getTotalAmount();
            totalAmountView.setText(String.format("%+,.2f€", totalAmount));
            int color = Color.BLACK;
            if (totalAmount < 0) {
                color = Color.parseColor("#F44336");
            } else if (totalAmount > 0) {
                color = Color.parseColor("#4CAF50");
            } else if (totalAmount == 0) {
                color = Color.parseColor("#FF9800");
            }
            totalAmountView.setTextColor(color);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }
    }
}
