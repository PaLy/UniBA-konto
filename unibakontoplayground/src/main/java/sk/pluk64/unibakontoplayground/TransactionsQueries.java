package sk.pluk64.unibakontoplayground;

import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sk.pluk64.unibakonto.UnibaKonto;

public class TransactionsQueries {
    static long mealsCount(List<UnibaKonto.Transaction> transactions) {
        return mealsCount(transactions, x -> true);
    }

    static long mealsCount(List<UnibaKonto.Transaction> transactions, Predicate<UnibaKonto.TransactionItem> tiFilter) {
        return transactionItems(transactions)
            .filter(ti -> "MEN".equals(ti.service))
            .filter(tiFilter)
            .count();
    }

    private static Map<String, Long> mostBoughtMeals(List<UnibaKonto.Transaction> transactions) {
        return mostBoughtMeals(transactions, x -> true);
    }

    static Map<String, Long> mostBoughtMeals(List<UnibaKonto.Transaction> transactions, Predicate<UnibaKonto.TransactionItem> tiFilter) {
        Map<String, Long> map = transactionItems(transactions)
            .filter(tiFilter)
            .filter(ti -> "MEN".equals(ti.service))
            .collect(
                Collectors.groupingBy(
                    ti -> ti.description,
                    Collectors.counting()
                )
            );

        return map.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (v1, v2) -> {
                    throw new IllegalStateException();
                },
                LinkedHashMap::new
            ));
    }

    static double avgTransactionFoodCost(List<UnibaKonto.Transaction> transactions) {
        return transactions.stream()
            .mapToDouble(t ->
                t.transactionItems.stream()
                    .filter(ti -> "MEN".equals(ti.service))
                    .mapToDouble(ti -> ti.parsedAmount)
                    .sum()
            ).filter(x -> x != 0)
            .average()
            .orElse(0);
    }

    static double totalRecharges(List<UnibaKonto.Transaction> transactions) {
        return sumAmountByService(transactions, "DOB");
    }

    static double totalAccommodationCost(List<UnibaKonto.Transaction> transactions) {
        return sumAmountByService(transactions, "UBY");
    }

    static double totalFoodCost(List<UnibaKonto.Transaction> transactions) {
        return sumAmountByService(transactions, "MEN");
    }

    private static double sumAmountByService(List<UnibaKonto.Transaction> transactions, String service) {
        return transactionItems(transactions)
            .filter(ti -> service.equals(ti.service))
            .mapToDouble(ti -> ti.parsedAmount)
            .sum();
    }

    private static Stream<UnibaKonto.TransactionItem> transactionItems(List<UnibaKonto.Transaction> transactions) {
        return transactions.stream()
            .map(t -> t.transactionItems)
            .flatMap(Collection::stream);
    }

    private static Map<String, List<String>> descriptionsByShortcut(List<UnibaKonto.Transaction> transactions) {
        return transactionItems(transactions)
            .collect(
                Collectors.groupingBy(
                    ti -> ti.shortcut,
                    Collectors.mapping(
                        ti -> ti.description,
                        Collectors.toList()
                    )
                )
            );
    }

    static long mealsTransactionsCount(List<UnibaKonto.Transaction> transactions) {
        return mealsTransactionsCount(transactions, x -> true);
    }

    static long mealsTransactionsCount(List<UnibaKonto.Transaction> transactions, Predicate<UnibaKonto.TransactionItem> tiFilter) {
        return transactions.stream()
            .filter(t -> t.transactionItems.stream().map(ti -> ti.service).anyMatch("MEN"::equals))
            .filter(t -> t.transactionItems.stream().anyMatch(tiFilter))
            .count();
    }

    static Map<Integer, Long> menzaVisitsByHour(List<UnibaKonto.Transaction> transactions) {
        Map<Integer, Long> map = transactions.stream()
            .filter(t -> t.transactionItems.stream().map(ti -> ti.service).anyMatch("MEN"::equals))
            .map(UnibaKonto.Transaction::getParsedTimestamp)
            .filter(Objects::nonNull)
            .map(date -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.SECOND, -calendar.get(Calendar.SECOND));
                calendar.add(Calendar.MINUTE, -calendar.get(Calendar.MINUTE));
                return calendar.getTime();
            })
            .distinct()
            .map(date -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                return calendar.get(Calendar.HOUR_OF_DAY);
            })
            .collect(
                Collectors.groupingBy(
                    Function.identity(),
                    Collectors.counting()
                )
            );

        return map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (v1, v2) -> {
                        throw new IllegalStateException();
                    },
                    LinkedHashMap::new
                )
            );
    }
}
