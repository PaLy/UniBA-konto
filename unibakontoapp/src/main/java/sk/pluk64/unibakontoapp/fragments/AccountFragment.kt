package sk.pluk64.unibakontoapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_account.view.*
import kotlinx.android.synthetic.main.refreshed_timestamp_row.view.*
import kotlinx.android.synthetic.main.transaction.view.*
import kotlinx.android.synthetic.main.transaction_item.view.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import sk.pluk64.unibakonto.*
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import sk.pluk64.unibakontoapp.preferencesutils.getBalances
import sk.pluk64.unibakontoapp.preferencesutils.getDate
import sk.pluk64.unibakontoapp.preferencesutils.getList
import java.util.*

class AccountFragment : Fragment(), Refreshable {
    lateinit var activity: MainActivity
        private set
    private lateinit var mView: View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    val preferences: SharedPreferences by lazy {
        activity.getPreferences(Context.MODE_PRIVATE)
    }

    private var balances: Balances = Balances.EMPTY
    private val mAdapter = MyAdapter()
    private var updateDataTask: AccountDataTask? = null
    private var cards: List<CardInfo>? = null
    private var refreshTime: Date? = null
    private var parentFragment: EwalletAndMenusFragment? = null
    private var refreshClientDataUiListener: RefreshClientDataUiListener? = null

    val swipeRefresh
        get() = mView.swipe_refresh

    override fun onDestroyView() {
        super.onDestroyView()
        updateDataTask?.cancel(true)
    }

    override fun refresh() {
        if (updateDataTask != null) {
            return
        }
        setRefreshing()
        activity.setLogoutButtonEnabled(false)
        parentFragment?.isForceRefresh = false

        val updateDataTask = AccountDataTask(activity, this)
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        this.updateDataTask = updateDataTask
    }

    override fun canRefresh() = true

    private fun saveData(balances: Balances, clientName: String, transactions: List<Transaction>, cards: List<CardInfo>) {
        val jsonBalances = Json.encodeToString(Balances.serializer(), balances)
        val jsonTransactions = Json.encodeToString(ListSerializer(Transaction.serializer()), transactions)
        val jsonCards = Json.encodeToString(ListSerializer(CardInfo.serializer()), cards)
        refreshTime = DateUtils.currentTime
        val jsonRefreshTime = Json.encodeToString(DateSerializer, refreshTime)

        preferences.edit()
            .putString(PreferencesKeys.BALANCES, jsonBalances)
            .putString(PreferencesKeys.CLIENT_NAME, clientName)
            .putString(PreferencesKeys.TRANSACTIONS, jsonTransactions)
            .putString(PreferencesKeys.CARDS, jsonCards)
            .putString(PreferencesKeys.ACCOUNT_REFRESH_TIMESTAMP, jsonRefreshTime)
            .putString(PreferencesKeys.TRANSACTIONS_REFRESH_TIMESTAMP, jsonRefreshTime)
            .apply()
    }

    private fun loadData() {
        val transactions = preferences.getList(PreferencesKeys.TRANSACTIONS, Transaction.serializer())
        mAdapter.data.clear()
        mAdapter.data.addAll(transactions)
        mAdapter.notifyDataSetChanged()

        balances = preferences.getBalances(PreferencesKeys.BALANCES)
        cards = preferences.getList(PreferencesKeys.CARDS, CardInfo.serializer())
        refreshTime = preferences.getDate(PreferencesKeys.ACCOUNT_REFRESH_TIMESTAMP)
    }

    private fun setRefreshing() {
        mView.refresh_timestamp.text = getString(R.string.refreshing)
    }

    private fun updateRefreshTime() {
        val refreshTimeFormatted = DateUtils.getReadableTime(refreshTime, getString(R.string.never), context!!)
        mView.refresh_timestamp.text = getString(R.string.refreshed, refreshTimeFormatted)
    }

    private fun updateViewBalances() {
        mapOf(
            R.id.text_balance to UnibaKonto.ID_ACCOUNT,
            R.id.text_deposit to UnibaKonto.ID_DEPOSIT,
            R.id.text_deposit2 to UnibaKonto.ID_DEPOSIT2,
            R.id.text_zaloha to UnibaKonto.ID_ZALOHA
        )
            .mapKeys { mView.findViewById<TextView>(it.key) }
            .mapValues { balances.map[it.value] }
            .forEach { (textView, data) ->
                if (data != null) {
                    textView.text = Utils.fromHtml("<b>" + data.label + "</b>" + " " + data.price)
                    textView.visibility = View.VISIBLE
                } else {
                    textView.text = ""
                    textView.visibility = View.GONE
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        parentFragment = getParentFragment() as EwalletAndMenusFragment?
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        mView = view

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mView.transactions_history.setHasFixedSize(true)
        val tLayoutManager = LinearLayoutManager(context)
        mView.transactions_history.layoutManager = tLayoutManager
        mView.transactions_history.adapter = mAdapter

        loadData()
        updateRefreshTime()

        mView.swipe_refresh.setOnRefreshListener { refresh() }
        updateViewBalances()
        return view
    }

    override fun onResume() {
        super.onResume()
        val parentFragment = parentFragment
        if (DateUtils.isTimeDiffMoreThanXHours(refreshTime, 2) || parentFragment != null && parentFragment.isForceRefresh) {
            mView.swipe_refresh.post { refresh() }
        }
    }

    fun onUpdateTaskFinished(success: Boolean, noInternet: Boolean, balances: Balances, clientName: String, updatedTransactions: List<Transaction>, cards: List<CardInfo>) {
        this.balances = balances
        this.cards = cards

        when {
            success -> {
                saveData(balances, clientName, updatedTransactions, cards)
                updateViewBalances()
                updateRefreshTime()
                refreshClientDataUiListener!!.refreshClientDataUI()
                val adapter = this@AccountFragment.mAdapter
                adapter.data.clear()
                adapter.data.addAll(updatedTransactions)
                adapter.notifyDataSetChanged()
                mView.swipe_refresh.isRefreshing = false
                activity.setLogoutButtonEnabled(true)
            }
            noInternet -> {
                activity.setLogoutButtonEnabled(true)
                mView.swipe_refresh.isRefreshing = false
                updateRefreshTime()
            }
            else -> {
                // from whatever reason card view and swipeRefresh stay on screen after fragment replacement
                // this workaround will hide it
                // TODO could potentially leaks memory?
                mView.balances_card_view.visibility = View.GONE
                mView.transactions_history.visibility = View.GONE
                mView.swipe_refresh.isRefreshing = false

                activity.isLoggedIn = false
                parentFragment?.removeFragment(this@AccountFragment)
            }
        }
        updateDataTask = null
    }

    fun onUpdateTaskCancelled() {
        updateDataTask = null
    }

    fun setRefreshClientDataUiListener(refreshClientDataUiListener: RefreshClientDataUiListener) {
        this.refreshClientDataUiListener = refreshClientDataUiListener
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        private val transactions = ArrayList<Transaction>()

        val data: MutableList<Transaction>
            get() = transactions

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder(val view: CardView) : RecyclerView.ViewHolder(view)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.transaction, parent, false)
            return ViewHolder(v as CardView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = transactions[position]
            val transactionsView = holder.view
            val context = transactionsView.context

            val transactionTable = transactionsView.transaction_table
            transactionTable.removeAllViews()
            for (transactionItem in transaction.transactionItems) {
                val transactionItemView = LayoutInflater.from(context)
                    .inflate(R.layout.transaction_item, transactionTable, false) as TableRow

                transactionItemView.transaction_item_description.text = transactionItem.description

                transactionItemView.transaction_item_price.text = String.format("%+,.2f€", transactionItem.parsedAmount)

                transactionTable.addView(transactionItemView)
            }

            transactionsView.transaction_timestamp.text = transaction.timestamp

            val totalAmountView = transactionsView.transaction_total
            val totalAmount = transaction.totalAmount
            totalAmountView.text = String.format("%+,.2f€", totalAmount)
            val color = when {
                totalAmount < 0 -> Color.parseColor("#F44336")
                totalAmount > 0 -> Color.parseColor("#4CAF50")
                totalAmount == 0.0 -> Color.parseColor("#FF9800")
                else -> Color.BLACK
            }
            totalAmountView.setTextColor(color)
        }

        override fun getItemCount() = transactions.size
    }
}
