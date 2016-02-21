package sk.pluk64.unibakonto;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.http.UnibaKonto;

public class AccountFragment extends Fragment {

    private Map<String, UnibaKonto.Balance> balances = Collections.emptyMap();
    private MyAdapter mAdapter = new MyAdapter();
    SwipeRefreshLayout swipeRefresh;
    private boolean wasRefreshed = false;

    public AccountFragment() {
    }

    private void updateData() {
        new AsyncTask<Void, Void, Boolean>() {
            private List<UnibaKonto.Transaction> updatedTransactions;

            @Override
            protected Boolean doInBackground(Void... params) {
                TabbedActivity activity = getMyActivity();
                if (activity == null) {
                    return false;
                } else {
                    UnibaKonto unibaKonto = activity.getUnibaKonto();
                    if (!unibaKonto.isLoggedIn(true)) {
                        unibaKonto.login();
                    }
                    if (unibaKonto.isLoggedIn()) {
                        balances = unibaKonto.getBalances();
                        updatedTransactions = unibaKonto.getTransactions();
                        return true;
                    } else {
                        return false;
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                View view = getView();
                if (success) {
                    wasRefreshed = true;
                    if (view != null) {
                        updateViewBalances(view);
                    }
                    MyAdapter adapter = AccountFragment.this.mAdapter;
                    adapter.getData().clear();
                    adapter.getData().addAll(updatedTransactions);
                    adapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                } else {
                    TabbedActivity activity = getMyActivity();
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
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            if (data != null) {
                TextView textView = (TextView) view.findViewById((Integer) vb[0]);
                textView.setText(Html.fromHtml("<b>" + data.label + "</b>" + " " + data.price));
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

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
        if (!wasRefreshed) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefresh.setRefreshing(true);
                    updateData();
                }
            });
        }
        updateViewBalances(view);
        return view;
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