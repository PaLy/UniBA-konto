package sk.pluk64.unibakonto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Transaction {
    public final List<TransactionItem> transactionItems = new ArrayList<>();

    Transaction(TransactionItem... transactionItems) {
        Collections.addAll(this.transactionItems, transactionItems);
    }

    public void add(TransactionItem item) {
        transactionItems.add(0, item); // TODO not effective
    }

    public String getTimestamp() {
        if (transactionItems.isEmpty()) {
            return "";
        } else {
            return transactionItems.get(transactionItems.size() - 1).timestamp;
        }
    }

    public Date getParsedTimestamp() {
        if (transactionItems.isEmpty()) {
            return null;
        } else {
            return transactionItems.get(transactionItems.size() - 1).getParsedTimestamp();
        }
    }

    public double getTotalAmount() {
        double res = 0;
        for (TransactionItem transactionItem : transactionItems) {
            res += transactionItem.parsedAmount;
        }
        return res;
    }
}
