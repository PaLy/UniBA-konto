package sk.pluk64.unibakonto;

import java.util.List;
import java.util.Map;

public interface IsUnibaKonto {
    String getUsername();

    String getPassword();

    void login() throws Util.ConnectionFailedException;

    Boolean isLoggedIn(boolean refresh);

    Boolean isLoggedIn();

    Map<String, Balance> getBalances() throws Util.ConnectionFailedException;

    String getClientName() throws Util.ConnectionFailedException;

    List<Transaction> getAllTransactions() throws Util.ConnectionFailedException;

    List<Transaction> getTransactions() throws Util.ConnectionFailedException;

    List<CardInfo> getCards() throws Util.ConnectionFailedException;
}
