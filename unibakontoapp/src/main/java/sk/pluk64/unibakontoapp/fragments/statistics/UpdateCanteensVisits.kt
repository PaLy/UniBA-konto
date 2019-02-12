package sk.pluk64.unibakontoapp.fragments.statistics

import com.anychart.chart.common.dataentry.DataEntry
import sk.pluk64.unibakonto.Transaction
import sk.pluk64.unibakonto.TransactionsQueries
import sk.pluk64.unibakontoapp.asynctask.AsyncTaskWithCallbacks

class UpdateCanteensVisits(
    private val transactions: List<Transaction>,
    callback: (List<DataEntry>) -> Unit
) : AsyncTaskWithCallbacks<Void, Void, List<DataEntry>>() {

    init {
        addCallback(callback)
    }

    override fun doInBackground(vararg params: Void): List<DataEntry> {
        return TransactionsQueries.canteensVisitsByHour(transactions)
            .map {
                val data = DataEntry()
                data.setValue("x", it.key)
                data.setValue("value", it.value)
                data.setValue("until", it.key + 1)
                data
            }
    }
}
