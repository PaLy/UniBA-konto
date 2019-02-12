package sk.pluk64.unibakontoapp.fragments.statistics

import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.NameValueDataEntry
import sk.pluk64.unibakonto.Transaction
import sk.pluk64.unibakonto.TransactionItem
import sk.pluk64.unibakonto.TransactionItemFilters
import sk.pluk64.unibakonto.TransactionsQueries
import sk.pluk64.unibakontoapp.R
import sk.pluk64.unibakontoapp.asynctask.AsyncTaskWithCallbacks

class UpdateMostEatenMeals(
    private val transactions: List<Transaction>,
    private val getString: (Int) -> String,
    callback: (List<DataEntry>) -> Unit
): AsyncTaskWithCallbacks<Void, Void, List<DataEntry>>() {

    init {
        addCallback(callback)
    }

    override fun doInBackground(vararg params: Void): List<DataEntry> {
        val createEntry: (Int, (TransactionItem) -> Boolean) -> DataEntry = { nameResourceId, filter ->
            NameValueDataEntry(
                nameResourceId.toString(),
                getString(nameResourceId),
                TransactionsQueries.mealsCount(transactions, filter)
            )
        }

        return listOf(
            createEntry(R.string.chicken, TransactionItemFilters::isChickenMeal),
            createEntry(R.string.pork, TransactionItemFilters::isPorkMeal),
            createEntry(R.string.beef, TransactionItemFilters::isBeefMeal),
            createEntry(R.string.rice, TransactionItemFilters::isRiceMeal),
            createEntry(R.string.potatoes, TransactionItemFilters::isPotatoMeal),
            createEntry(R.string.soup, TransactionItemFilters::isSoup),
            createEntry(R.string.salad, TransactionItemFilters::isSaladMeal),
            createEntry(R.string.turkey, TransactionItemFilters::isTurkeyMeal),
            createEntry(R.string.cheese, TransactionItemFilters::isCheeseMeal),
            createEntry(R.string.drink, TransactionItemFilters::isDrink),
            createEntry(R.string.fish, TransactionItemFilters::isFishMeal),
            createEntry(R.string.other, TransactionItemFilters::isOtherMeal),
            createEntry(R.string.knodel, TransactionItemFilters::isKnodel),
            createEntry(R.string.eggBarley, TransactionItemFilters::isEggBarley)
        )
            .sortedBy { it.getValue("value").toString().toInt() }
    }
}