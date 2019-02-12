package sk.pluk64.unibakonto

import java.util.*

object TransactionsQueries {

    fun mealsCount(transactions: List<Transaction>, tiFilter: (TransactionItem) -> Boolean = { true }): Int {
        return transactionItems(transactions)
            .filter { ti -> "MEN" == ti.service }
            .filter(tiFilter)
            .count()
    }

    private fun mostBoughtMeals(transactions: List<Transaction>): SortedMap<String, Int> {
        return mostBoughtMeals(transactions) { true }
    }

    fun mostBoughtMeals(transactions: List<Transaction>, tiFilter: (TransactionItem) -> Boolean): SortedMap<String, Int> {
        return transactionItems(transactions)
            .filter(tiFilter)
            .filter { "MEN" == it.service }
            .groupingBy { it.description }
            .eachCount()
            .toSortedMap(reverseOrder())
    }

    fun avgTransactionFoodCost(transactions: List<Transaction>): Double {
        return transactions.map { transaction ->
            transaction.transactionItems.asSequence()
                .filter { "MEN" == it.service }
                .map { it.parsedAmount }
                .filterNotNull()
                .sum()
        }
            .average()
    }

    fun totalRecharges(transactions: List<Transaction>): Double {
        return sumAmountByService(transactions, "DOB")
    }

    fun totalAccommodationCost(transactions: List<Transaction>): Double {
        return sumAmountByService(transactions, "UBY")
    }

    fun totalFoodCost(transactions: List<Transaction>): Double {
        return sumAmountByService(transactions, "MEN")
    }

    private fun sumAmountByService(transactions: List<Transaction>, service: String): Double {
        return transactionItems(transactions)
            .filter { service == it.service }
            .map { it.parsedAmount }
            .filterNotNull()
            .sum()
    }

    private fun transactionItems(transactions: List<Transaction>): Sequence<TransactionItem> {
        return transactions.asSequence().map { it.transactionItems }.flatten()
    }

    private fun descriptionsByShortcut(transactions: List<Transaction>): Map<String, List<String>> {
        return transactionItems(transactions)
            .groupBy { it.shortcut }
            .mapValues { it.value.map { it.description } }
    }

    @JvmOverloads
    fun mealsTransactionsCount(transactions: List<Transaction>, tiFilter: (TransactionItem) -> Boolean = { true }): Int {
        return transactions
            .filter { it.transactionItems.any { "MEN" == it.service } }
            .filter { it.transactionItems.any(tiFilter) }
            .count()
    }

    fun mealsTransactionsCount(transactions: List<Transaction>, tiFilters: List<(TransactionItem) -> Boolean>): Int {
        return transactions
            .filter { it.transactionItems.any { "MEN" == it.service } }
            .filter { t -> tiFilters.all { t.transactionItems.any(it) } }
            .count()
    }

    fun canteensVisitsByHour(transactions: List<Transaction>): SortedMap<Int, Int> {
        return transactions.asSequence()
            .filter { it.transactionItems.any { "MEN" == it.service } }
            .map { it.parsedTimestamp }
            .filterNotNull()
            .map {
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.add(Calendar.SECOND, -calendar.get(Calendar.SECOND))
                calendar.add(Calendar.MINUTE, -calendar.get(Calendar.MINUTE))
                calendar.time
            }
            .distinct()
            .map {
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.get(Calendar.HOUR_OF_DAY)
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }
}
