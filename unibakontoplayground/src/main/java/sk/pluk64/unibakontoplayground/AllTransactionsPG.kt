package sk.pluk64.unibakontoplayground

import com.google.common.base.Joiner
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import sk.pluk64.unibakonto.*
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

fun main() {
    //        exportAllTransactions(readLogin());

    val transactions = importTransactions("all_transactions.json")

    //        writeAsJsonToFile(descriptionsByShortcut(transactions), "descriptions_by_shortcut.json");

    val totalFoodCost = TransactionsQueries.totalFoodCost(transactions)
    val totalAccommodationCost = TransactionsQueries.totalAccommodationCost(transactions)
    val totalRecharges = TransactionsQueries.totalRecharges(transactions)
    val avgTransactionFoodCost = TransactionsQueries.avgTransactionFoodCost(transactions)
    println(String.format("Food: %.2f", totalFoodCost))
    println(String.format("Accommodation: %.2f", totalAccommodationCost))
    println(String.format("Recharges: %.2f", totalRecharges))
    println(String.format("Avg. transaction food cost: %.2f", avgTransactionFoodCost))

    println("Most bought meals:")
    val meals = TransactionsQueries.mostBoughtMeals(transactions) { ti: TransactionItem -> TransactionItemFilters.isChickenMeal(ti) }
    printMap(meals)

    println(String.format("Total meals transactions: %d", TransactionsQueries.mealsTransactionsCount(transactions)))
    println(String.format("Total chicken transactions: %d", TransactionsQueries.mealsTransactionsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isChickenMeal(ti) }))
    println(String.format("Total meals: %d", TransactionsQueries.mealsCount(transactions)))
    println(String.format("Chicken meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isChickenMeal(ti) }))
    println(String.format("Beef meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isBeefMeal(ti) }))
    println(String.format("Pork meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isPorkMeal(ti) }))
    println(String.format("Turkey meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isTurkeyMeal(ti) }))
    println(String.format("Soups: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isSoup(ti) }))
    println(String.format("Rice meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isRiceMeal(ti) }))
    println(String.format("Potato meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isPotatoMeal(ti) }))
    println(String.format("Cheese meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isCheeseMeal(ti) }))
    println(String.format("Salad meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isSaladMeal(ti) }))
    println(String.format("Drinks: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isDrink(ti) }))
    println(String.format("Knödels: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isKnodel(ti) }))
    println(String.format("Egg barleys: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isEggBarley(ti) }))
    println(String.format("Fish meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isFishMeal(ti) }))
    println(String.format("Other meals: %d", TransactionsQueries.mealsCount(transactions) { ti: TransactionItem -> TransactionItemFilters.isOtherMeal(ti) }))

    println("Transactions by hour:")
    printMap(TransactionsQueries.canteensVisitsByHour(transactions))
}

private fun printMap(map: Map<*, *>) {
    println(Joiner.on("\n").withKeyValueSeparator(": ").join(map))
}

private fun readLogin(): UnibaKonto {
    val scanner = Scanner(System.`in`)
    val username = scanner.nextLine()
    val password = scanner.nextLine()
    return UnibaKonto(username, password)
}

private fun importTransactions(filepath: String): List<Transaction> {
    var file = ByteArray(0)
    try {
        file = Files.readAllBytes(Paths.get(filepath))
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val transactions = String(file, Charset.defaultCharset())
    return Gson().fromJson(
        transactions,
        object : TypeToken<List<Transaction>>() {

        }.type
    )
}

private fun exportAllTransactions(unibaKonto: UnibaKonto) {
    try {
        unibaKonto.login()
        if (unibaKonto.isLoggedIn) {
            val allTransactions = unibaKonto.allTransactions
            writeAsJsonToFile(allTransactions, "all_transactions.json")
        }
    } catch (e: Util.ConnectionFailedException) {
        e.printStackTrace()
    }

}

private fun writeAsJsonToFile(`object`: Any, filename: String) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(`object`)

    try {
        PrintWriter(filename).use { out -> out.println(json) }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }

}

