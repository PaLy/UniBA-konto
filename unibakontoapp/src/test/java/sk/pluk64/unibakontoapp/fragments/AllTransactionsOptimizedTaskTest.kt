package sk.pluk64.unibakontoapp.fragments

import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import sk.pluk64.unibakonto.IsUnibaKonto
import sk.pluk64.unibakonto.Transaction
import sk.pluk64.unibakonto.TransactionItem
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.PreferencesKeys
import sk.pluk64.unibakontoapp.fragments.statistics.AllTransactionsOptimizedTask
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import java.util.*

class AllTransactionsOptimizedTaskTest {

    @Test
    fun testMerge() {
        val t1 = Transaction(listOf(TransactionItem(shortcut = "1")))
        val t1c = Transaction(listOf(TransactionItem(shortcut = "1")))
        val t2 = Transaction(listOf(TransactionItem(shortcut = "2")))
        val t2c = Transaction(listOf(TransactionItem(shortcut = "2")))
        val t3 = Transaction(listOf(TransactionItem(shortcut = "3")))
        val t3c = Transaction(listOf(TransactionItem(shortcut = "3")))
        val recentlyRefreshed = Json.stringify(DateSerializer, Calendar.getInstance().time)
        val oldTime = Calendar.getInstance()
        oldTime.set(Calendar.YEAR, 2000)
        val notRecentlyRefreshed = Json.stringify(DateSerializer, oldTime.time)

        assertEquals(listOf(t1, t2, t3),
            testMerge(
                allRefreshed = listOf(t1, t2, t3)
            )
        )
        assertEquals(listOf(t1, t2, t3),
            testMerge(
                allOld = listOf(t1),
                lastRefreshed = listOf(t1c, t2c, t3c),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
        assertEquals(listOf(t1, t2, t3),
            testMerge(
                allRefreshed = emptyList(),
                lastRefreshed = listOf(t1, t2, t3),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
        // old empty
        assertEquals(listOf(t2),
            testMerge(
                allRefreshed = listOf(t1),
                lastRefreshed = listOf(t2c),
                allOld = emptyList(),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
        // this should not happen (last different from allRefreshed)
        assertEquals(listOf(t1),
            testMerge(
                allRefreshed = listOf(t1),
                lastRefreshed = listOf(t2c),
                allOld = emptyList(),
                allRefreshTimestamp = notRecentlyRefreshed
            )
        )
        // without all transactions refresh
        assertEquals(listOf(t1, t1, t2),
            testMerge(
                allRefreshed = emptyList(),
                lastRefreshed = listOf(t1c, t1c, t2c),
                allOld = listOf(t1),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
        // me recent
        assertEquals(listOf(t1),
            testMerge(
                allRefreshed = emptyList(),
                lastRefreshed = emptyList(),
                allOld = listOf(t1),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
        // me not recent
        assertEquals(listOf(t1, t2),
            testMerge(
                allRefreshed = listOf(t1c, t2c),
                lastRefreshed = emptyList(),
                allOld = listOf(t1),
                allRefreshTimestamp = notRecentlyRefreshed
            )
        )
        assertEquals(listOf(t1, t2, t3),
            testMerge(
                allOld = listOf(t1, t2, t3),
                lastRefreshed = listOf(t2, t3),
                allRefreshTimestamp = recentlyRefreshed
            )
        )
    }

    private fun testMerge(allRefreshed: List<Transaction> = emptyList(),
                          lastRefreshed: List<Transaction> = emptyList(),
                          allOld: List<Transaction> = emptyList(),
                          allRefreshTimestamp: String = ""): List<Transaction>? {
        val preferencesEdit = mock<SharedPreferences.Editor> {
            on { putString(anyString(), any()) } doReturn it
        }
        val preferencesMock = mock<SharedPreferences> {
            on { getString(eq(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP), anyString()) } doReturn allRefreshTimestamp
            on { getString(eq(PreferencesKeys.ALL_TRANSACTIONS), anyString()) } doReturn Json.stringify(Transaction.serializer().list, allOld)
            on { edit() } doReturn preferencesEdit
        }
        val unibaKontoMock = mock<IsUnibaKonto> {
            on { this.allTransactions } doReturn allRefreshed
            on { transactions } doReturn lastRefreshed
            on { isLoggedIn } doReturn true
        }
        val activity = mock<MainActivity> {
            on { unibaKonto } doReturn unibaKontoMock
        }

        return AllTransactionsOptimizedTask(activity, preferencesMock) {}.doInBackground()
    }
}