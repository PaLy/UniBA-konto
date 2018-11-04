package sk.pluk64.unibakontoplayground;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sk.pluk64.unibakonto.UnibaKonto;
import sk.pluk64.unibakonto.Util;

public class AllTransactionsPG {
    public static void main(String[] args) {
//        exportAllTransactions(readLogin());

        List<UnibaKonto.Transaction> transactions = importTransactions("all_transactions.json");

//        writeAsJsonToFile(descriptionsByShortcut(transactions), "descriptions_by_shortcut.json");

        double totalFoodCost = TransactionsQueries.totalFoodCost(transactions);
        double totalAccommodationCost = TransactionsQueries.totalAccommodationCost(transactions);
        double totalRecharges = TransactionsQueries.totalRecharges(transactions);
        double avgTransactionFoodCost = TransactionsQueries.avgTransactionFoodCost(transactions);
        System.out.println(String.format("Food: %.2f", totalFoodCost));
        System.out.println(String.format("Accommodation: %.2f", totalAccommodationCost));
        System.out.println(String.format("Recharges: %.2f", totalRecharges));
        System.out.println(String.format("Avg. transaction food cost: %.2f", avgTransactionFoodCost));

        System.out.println("Most bought meals:");
        Map<String, Long> meals = TransactionsQueries.mostBoughtMeals(transactions, TransactionItemFilters::isChickenMeal);
        printMap(meals);

        System.out.println(String.format("Total meals transactions: %d", TransactionsQueries.mealsTransactionsCount(transactions)));
        System.out.println(String.format("Total chicken transactions: %d", TransactionsQueries.mealsTransactionsCount(transactions, TransactionItemFilters::isChickenMeal)));
        System.out.println(String.format("Total meals: %d", TransactionsQueries.mealsCount(transactions)));
        System.out.println(String.format("Chicken meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isChickenMeal)));
        System.out.println(String.format("Beef meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isBeefMeal)));
        System.out.println(String.format("Pork meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isPorkMeal)));
        System.out.println(String.format("Turkey meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isTurkeyMeal)));
        System.out.println(String.format("Soups: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isSoup)));
        System.out.println(String.format("Rice meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isRiceMeal)));
        System.out.println(String.format("Potato meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isPotatoMeal)));
        System.out.println(String.format("Cheese meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isCheeseMeal)));
        System.out.println(String.format("Salad meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isSaladMeal)));
        System.out.println(String.format("Drinks: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isDrink)));
        System.out.println(String.format("Knödels: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isKnodel)));
        System.out.println(String.format("Egg barleys: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isEggBarley)));
        System.out.println(String.format("Fish meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isFishMeal)));
        System.out.println(String.format("Other meals: %d", TransactionsQueries.mealsCount(transactions, TransactionItemFilters::isOtherMeal)));

        System.out.println("Transactions by hour:");
        printMap(TransactionsQueries.menzaVisitsByHour(transactions));
    }

    private static void printMap(Map<?, ?> map) {
        System.out.println(Joiner.on("\n").withKeyValueSeparator(": ").join(map));
    }

    private static UnibaKonto readLogin() {
        Scanner scanner = new Scanner(System.in);
        String username = scanner.nextLine();
        String password = scanner.nextLine();
        return new UnibaKonto(username, password);
    }

    private static List<UnibaKonto.Transaction> importTransactions(String filepath) {
        byte[] file = new byte[0];
        try {
            file = Files.readAllBytes(Paths.get(filepath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String transactions = new String(file, Charset.defaultCharset());
        return new Gson().fromJson(
            transactions,
            new TypeToken<List<UnibaKonto.Transaction>>() {
            }.getType()
        );
    }

    private static void exportAllTransactions(UnibaKonto unibaKonto) {
        try {
            unibaKonto.login();
            if (unibaKonto.isLoggedIn()) {
                List<UnibaKonto.Transaction> allTransactions = unibaKonto.getAllTransactions();
                writeAsJsonToFile(allTransactions, "all_transactions.json");
            }
        } catch (Util.ConnectionFailedException e) {
            e.printStackTrace();
        }
    }

    private static void writeAsJsonToFile(Object object, String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(object);

        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

