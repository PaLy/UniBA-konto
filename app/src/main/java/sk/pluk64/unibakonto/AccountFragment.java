package sk.pluk64.unibakonto;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sk.pluk64.unibakonto.http.UnibaKonto;

public class AccountFragment extends Fragment {
    private String balance = "";
    private List<UnibaKonto.Transaction> transactions = new ArrayList<>();

    public AccountFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO add spinner
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                UnibaKonto unibaKonto = LoginActivity.unibaKonto;
                transactions = unibaKonto.getTransactions();
                balance = unibaKonto.getBalance();
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                View view = getView();
                if (view != null) {
                    updateViewData(view);
                }
            }
        }.execute();
    }

    private void updateViewData(View view) {
        TextView textView = (TextView) view.findViewById(R.id.info_text);
        textView.setText("Balance: " + balance);

        RecyclerView transactionsView = (RecyclerView) view.findViewById(R.id.transactions_history);
        RecyclerView.Adapter mAdapter = new MyAdapter(transactions);
        transactionsView.setAdapter(mAdapter);
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

        updateViewData(view);
        return view;
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<UnibaKonto.Transaction> transactions;

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
        public MyAdapter(List<UnibaKonto.Transaction> transactions) {
            this.transactions = transactions;
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

                TextView amountView = (TextView) transactionItemView.findViewById(R.id.transaction_item_amount);
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
