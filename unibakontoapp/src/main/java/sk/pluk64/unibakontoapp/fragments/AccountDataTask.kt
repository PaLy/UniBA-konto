package sk.pluk64.unibakontoapp.fragments

import android.os.AsyncTask
import sk.pluk64.unibakonto.*
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.Utils
import java.lang.ref.WeakReference

internal class AccountDataTask(myActivity: MainActivity, accountFragment: AccountFragment) : AsyncTask<Void, Void, Boolean>() {
    private val activityReference: WeakReference<MainActivity> = WeakReference(myActivity)
    private val fragmentReference: WeakReference<AccountFragment> = WeakReference(accountFragment)
    private var noInternet = false
    private var updatedTransactions: List<Transaction> = emptyList()
    private var balances: Balances = Balances.EMPTY
    private var clientName: String = ""
    private var cards: List<CardInfo> = emptyList()

    private fun showNoInternetConnectionToastFromBackgroundThread() {
        val activity = activityReference.get()
        activity?.runOnUiThread { Utils.showNoInternetConnection(activity.applicationContext) }
    }

    override fun doInBackground(vararg params: Void): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            return false
        } else {
            val unibaKonto = activity.unibaKonto
            if (!unibaKonto.isLoggedIn(true)) {
                try {
                    unibaKonto.login()
                } catch (e: Util.ConnectionFailedException) {
                    noInternet = true
                    showNoInternetConnectionToastFromBackgroundThread()
                }

            }
            return if (unibaKonto.isLoggedIn) {
                try {
                    balances = unibaKonto.balances
                    clientName = unibaKonto.clientName
                    updatedTransactions = unibaKonto.transactions
                    cards = unibaKonto.cards
                    true
                } catch (e: Util.ConnectionFailedException) {
                    noInternet = true
                    showNoInternetConnectionToastFromBackgroundThread()
                    false
                }
            } else {
                false
            }
        }
    }

    override fun onPostExecute(success: Boolean) {
        val fragment = fragmentReference.get()
        fragment?.onUpdateTaskFinished(success, noInternet, balances, clientName, updatedTransactions, cards)
    }

    override fun onCancelled(success: Boolean) {
        val fragment = fragmentReference.get()
        fragment?.onUpdateTaskCancelled()
    }
}
