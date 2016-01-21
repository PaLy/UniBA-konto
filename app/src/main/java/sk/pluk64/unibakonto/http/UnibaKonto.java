package sk.pluk64.unibakonto.http;

import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class UnibaKonto {
    private static final String MOJA_UNIBA_LOGIN_PAGE = "https://moja.uniba.sk//cosauth/cosauth.php";
    private static final String UNIBA_LOGIN_PAGE = "https://login.uniba.sk/cosign.cgi";
    private static final String KONTO_LOGIN_PAGE = "https://konto.uniba.sk/";

    private static final String CLIENT_INF_PAGE = "https://konto.uniba.sk/Secure/UserAccount.aspx";
    public static final String ID_ACCOUNT = "#ctl00_ContentPlaceHolderMain_lblAccount";
    public static final String ID_DEPOSIT = "#ctl00_ContentPlaceHolderMain_lblFund";
    public static final String ID_DEPOSIT2 = "#ctl00_ContentPlaceHolderMain_lblFund2";
    public static final String ID_ZALOHA = "#ctl00_ContentPlaceHolderMain_lblZaloha";
    private static final String ID_VAR_SYMBOL = "#ctl00_ContentPlaceHolderMain_lblVarSymbol";

    private static final String TRANSACTIONS_PAGE = "https://konto.uniba.sk/Secure/Operace.aspx";
    private static final String ID_TRANSACTIONS_HISTORY = "#ctl00_ContentPlaceHolderMain_gvAccountHistory";

    private final String username;
    private final String password;
    private final ParsedDocumentCache documents = new ParsedDocumentCache();

    public UnibaKonto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void login() {
        // TODO remove workaround for ssl
        try {
            expiredCertificateWorkAround();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }

        CookieHandler.setDefault(new CookieManager());
        try {
            httpGet(MOJA_UNIBA_LOGIN_PAGE); // sets cookie
            mojaUnibaLogin();
            kontoLogin(); // TODO toto treba spravit asi po nejakom case opakovane???
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void expiredCertificateWorkAround() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
// Initialise the TMF as you normally would, for example:
        tmf.init((KeyStore) null);

        TrustManager[] trustManagers = tmf.getTrustManagers();
        final X509TrustManager origTrustmanager = (X509TrustManager) trustManagers[0];

        TrustManager[] wrappedTrustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return origTrustmanager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            origTrustmanager.checkClientTrusted(certs, authType);
                        } catch (CertificateException e) {
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            origTrustmanager.checkServerTrusted(certs, authType);
                        } catch (CertificateException e) {
                        }
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, wrappedTrustManagers, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private static void kontoLogin() throws IOException {
        URLConnection kontoLoginPageConn = httpGet(KONTO_LOGIN_PAGE); // sets cookie

        KontoParsedData parsedData = new KontoParsedData(kontoLoginPageConn);
        httpPost(KONTO_LOGIN_PAGE + parsedData.action, parsedData.postData);
    }

    public Boolean isLoggedIn() {
        // TODO pozor - ak sa zacachuje po zadani zleho hesla, tak potrebujem ho pri dalsom pokuse refresnut
        return !documents.get(CLIENT_INF_PAGE).select(ID_VAR_SYMBOL).isEmpty();
    }

    private void mojaUnibaLogin() throws IOException {
        // TODO it would be better to parse these data from login page
        httpPost(UNIBA_LOGIN_PAGE,
                Util.paramsArray2PostData(new String[]{
                        "ref", MOJA_UNIBA_LOGIN_PAGE,
                        "login", username,
                        "password", password
                })
        );
    }

    public static URLConnection httpGet(String location) throws IOException {
        URLConnection conn = new URL(location).openConnection();
        conn.connect();
        conn.getHeaderFields();
        return conn;
    }

    private static URLConnection httpPost(String location, byte[] postData) throws IOException {
        URLConnection conn = new URL(location).openConnection();
        conn.setDoOutput(true);
        new DataOutputStream(conn.getOutputStream()).write(postData);
        conn.connect();
        conn.getHeaderFields();
        return conn;
    }

    private static void printCookies() {
        System.out.println(((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies());
    }

    public Map<String, Balance> getBalances() {
        Map<String, Balance> result = new LinkedHashMap<>();
        Document doc = documents.get(CLIENT_INF_PAGE);

        String[] ids = {ID_ACCOUNT, ID_DEPOSIT, ID_DEPOSIT2, ID_ZALOHA};

        for (String id : ids) {
            Elements valueElem = doc.select(id);
            String label = valueElem.get(0).previousElementSibling().text();
            String price = valueElem.text();
            String condensedPrice = TextUtils.join("", price.split(" "));
            result.put(id, new Balance(label, condensedPrice));
        }

        return result;
    }

    public class Balance {
        public final String label;
        public final String price;

        private Balance(String label, String price) {
            this.label = label;
            this.price = price;
        }
    }

    public List<Transaction> getTransactions() {
        Elements table = documents.get(TRANSACTIONS_PAGE).select(ID_TRANSACTIONS_HISTORY);
        Elements tableRows = table.first().child(0).children();

        ArrayList<TransactionItem> items = new ArrayList<>();
        for (int i = 1; i < tableRows.size(); i++) {
            items.add(new TransactionItem(tableRows.get(i)));
        }

        ArrayList<Transaction> result = new ArrayList<>();
        if (!items.isEmpty()) {
            result.add(new Transaction(items.get(0)));
        }
        for (int i = 1; i < items.size(); i++) {
            TransactionItem curItem = items.get(i);
            long curItemTime = curItem.parsedTimestamp.getTime();
            long prevItemTime = items.get(i - 1).parsedTimestamp.getTime();

            if (Math.abs(curItemTime - prevItemTime) < 10 * 1000) { // 10 seconds
                result.get(result.size() - 1).add(curItem);
            } else {
                result.add(new Transaction(curItem));
            }
        }
        return result;
    }

    private static class KontoParsedData {
        public final byte[] postData;
        public final String action;

        public KontoParsedData(URLConnection parseFrom) throws IOException {
            Document kontoDoc = Jsoup.parse(Util.connInput2String(parseFrom));
            ArrayList<String> paramsArray = new ArrayList<>();
            for (Element input : kontoDoc.select("input")) {
                if (input.hasAttr("value")) {
                    paramsArray.add(URLEncoder.encode(input.attr("name"), "UTF-8"));
                    paramsArray.add(URLEncoder.encode(input.attr("value"), "UTF-8"));
                }
            }
            postData = Util.paramsArray2PostData(
                    paramsArray.toArray(new String[paramsArray.size()])
            );
            action = kontoDoc.select("form").attr("action");
        }
    }

    private static class ParsedDocumentCache {
        private final Map<String, Document> cache = new HashMap<>();

        public Document get(String location) {
            Document document = cache.get(location);
            if (document == null) {
                refresh(location);
                document = cache.get(location);
            }
            return document;
        }

        private void refresh(String location) {
            String html;
            try {
                URLConnection connection = httpGet(location);
                html = Util.connInput2String(connection);
            } catch (IOException e) {
                e.printStackTrace(); // TODO
                return;
            }
            Document document = Jsoup.parse(html);
            cache.put(location, document);
        }

        public Document getRefreshed(String location) {
            refresh(location);
            return get(location);
        }
    }

    public static class TransactionItem {
        public final String timestamp;
        private Date parsedTimestamp;
        private final String service;
        private final String shortcut;
        public final String description;
        public final String amount;
        public final double parsedAmount;
        private final String method;
        private final String obj;
        private final String payed;

        private static SimpleDateFormat dateFormat = new SimpleDateFormat("d. MM. yyyy HH:mm:ss");

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

            try {
                parsedTimestamp = dateFormat.parse(timestamp);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            parsedAmount = Double.parseDouble(amount.replace(',', '.'));
        }
    }

    public class Transaction {
        public final List<TransactionItem> transactionItems = new ArrayList<>();

        public Transaction(TransactionItem... transactionItems) {
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

        public double getTotalAmount() {
            double res = 0;
            for (TransactionItem transactionItem : transactionItems) {
                res += transactionItem.parsedAmount;
            }
            return res;
        }
    }
}
