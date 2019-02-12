package sk.pluk64.unibakontoapp.fragments.statistics

import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import sk.pluk64.unibakonto.Transaction
import sk.pluk64.unibakontoapp.DateUtils
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.PreferencesKeys
import sk.pluk64.unibakontoapp.fragments.UnibaKontoAsyncTask
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import sk.pluk64.unibakontoapp.preferencesutils.getDate
import sk.pluk64.unibakontoapp.preferencesutils.getList
import kotlin.math.min

class AllTransactionsOptimizedTask(
    activity: MainActivity,
    private val preferences: SharedPreferences,
    private val onFinishCallback: (List<Transaction>) -> Unit) : UnibaKontoAsyncTask<List<Transaction>>(activity) {

    private val unibaKonto = activity.unibaKonto
    private val isGetAll by lazy {
        val allTransactionsRefreshTime = preferences.getDate(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP)
        val isAllTransactionsOld = DateUtils.notThisMonth(allTransactionsRefreshTime)
        isAllTransactionsOld
    }

    override fun load(): List<Transaction>? {
        val transactions = if (isGetAll) {
            unibaKonto.allTransactions
        } else {
            unibaKonto.transactions
        }

        val jsonTransactions = Json.stringify(Transaction.serializer().list, transactions)
        val jsonCurrentTime = Json.stringify(DateSerializer, DateUtils.currentTime)

        if (isGetAll) {
            preferences.edit()
                .putString(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP, jsonCurrentTime)
                .putString(PreferencesKeys.ALL_TRANSACTIONS, jsonTransactions)
                .apply()
        } else {
            preferences.edit()
                .putString(PreferencesKeys.TRANSACTIONS_REFRESH_TIMESTAMP, jsonCurrentTime)
                .putString(PreferencesKeys.TRANSACTIONS, jsonTransactions)
                .apply()
        }

        return if (isGetAll) {
            transactions
        } else {
            merge(loadOldAllTransactions(), transactions)
        }
    }

    override fun onFinish(result: List<Transaction>?) {
        this.onFinishCallback(result ?: emptyList())
    }

    private fun loadOldAllTransactions(): List<Transaction> {
        return preferences.getList(PreferencesKeys.ALL_TRANSACTIONS, Transaction.serializer())
    }

    private fun merge(oldAllTransactions: List<Transaction>, lastTransactions: List<Transaction>): List<Transaction> {
        for (i in min(lastTransactions.size, oldAllTransactions.size) downTo 1) {
            if (oldAllTransactions.subList(oldAllTransactions.size - i, oldAllTransactions.size)
                == lastTransactions.subList(0, i)) {
                return oldAllTransactions + lastTransactions.subList(i, lastTransactions.size)
            }
        }

        preferences.edit()
            .putString(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP, null)
            .apply()

        return oldAllTransactions + lastTransactions
    }
}
