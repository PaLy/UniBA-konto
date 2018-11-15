package sk.pluk64.unibakonto;

import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class TransactionItem {
    public final String timestamp;
    private transient Date parsedTimestamp;
    public final String service;
    public final String shortcut;
    public final String description;
    public final String amount;
    public final double parsedAmount;
    public final String method;
    public final String obj;
    public final String payed;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("d. MM. yyyy HH:mm:ss");

    public TransactionItem(Element tableRow) {
        Iterator<Element> columns = tableRow.children().iterator();

        timestamp = columns.next().text();
        service = columns.next().text();
        shortcut = columns.next().text();
        description = columns.next().text();
        amount = columns.next().text();
        method = columns.next().text();
        obj = columns.next().text();
        payed = columns.next().text();

        parsedAmount = Double.parseDouble(amount.replace(',', '.'));
    }

    public Date getParsedTimestamp() {
        if (parsedTimestamp == null) {
            try {
                parsedTimestamp = dateFormat.parse(timestamp);
            } catch (ParseException e) {
                e.printStackTrace();
                return Calendar.getInstance().getTime();
            }
        }
        return parsedTimestamp;
    }
}
