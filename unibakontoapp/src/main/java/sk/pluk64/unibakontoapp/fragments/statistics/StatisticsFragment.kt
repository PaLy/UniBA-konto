package sk.pluk64.unibakontoapp.fragments.statistics

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.anychart.APIlib
import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import kotlinx.android.synthetic.main.statistics.view.*
import kotlinx.android.synthetic.main.statistics_card.view.*
import sk.pluk64.unibakonto.Transaction
import sk.pluk64.unibakonto.TransactionsQueries
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.asynctask.FunAsyncTask
import sk.pluk64.unibakontoapp.asynctask.ParallelAsyncTasks
import sk.pluk64.unibakontoapp.preferencesutils.getDate
import sk.pluk64.unibakontoapp.preferencesutils.getList


class StatisticsFragment : Fragment(), Refreshable {

    private lateinit var activity: MainActivity
    private lateinit var mView: View

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    private val preferences: SharedPreferences by lazy {
        activity.getPreferences(Context.MODE_PRIVATE)
    }

    private fun updateCanteensVisitsUI(entries: List<DataEntry>) {
        APIlib.getInstance().setActiveAnyChartView(mView.canteenVisitsHistogram)

        val chart = AnyChart.column()
        val series = chart.column(entries)
        series.name(getString(R.string.visitsCount))
        series.tooltip().titleFormat("{%x}:00 – {%until}:00")
        chart.title(getString(R.string.whenIVisitCanteens))

        mView.canteenVisitsHistogram.setChart(chart)
    }

    private fun updateMostEatenMealsUI(entries: List<DataEntry>) {
        APIlib.getInstance().setActiveAnyChartView(mView.foodTypes)

        val chart = AnyChart.venn()
        chart.data(entries)
        chart.tooltip().format(getString(R.string.count) + ": {%value}")
        chart.labels().fontColor("#757575")
        chart.title(getString(R.string.whatIEatMostly))

        mView.foodTypes.setChart(chart)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.statistics, container, false)
        mView = view

        view.totalSpent.header.text = getString(R.string.total_spent)
        view.onFood.header.text = getString(R.string.on_food)
        view.onAccommodation.header.text = getString(R.string.on_accommodation)
        view.onOther.header.text = getString(R.string.on_other)

        view.swipeRefresh.setOnRefreshListener { refresh() }

        loadData()

        return view
    }

    override fun onResume() {
        super.onResume()

        val accountRefreshTime = preferences.getDate(PreferencesKeys.TRANSACTIONS_REFRESH_TIMESTAMP)
        val isAccountDataOld = DateUtils.isTimeDiffMoreThanXHours(accountRefreshTime, 2)

        val allTransactionsRefreshTime = preferences.getDate(PreferencesKeys.ALL_TRANSACTIONS_REFRESH_TIMESTAMP)
        val isAllTransactionsOld = DateUtils.notThisMonth(allTransactionsRefreshTime)

        if (isAccountDataOld || isAllTransactionsOld) {
            refresh()
        }
    }

    private var updateDataTask: AllTransactionsOptimizedTask? = null

    override fun refresh() {
        if (updateDataTask != null) {
            return
        }

        mView.swipeRefresh.isRefreshing = true

        val updateDataTask = AllTransactionsOptimizedTask(activity, preferences, this::onUpdateDataTaskFinished)
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        this.updateDataTask = updateDataTask
    }

    private fun onUpdateDataTaskFinished(transactions: List<Transaction>) {
        updateDataTask = null
        if (isAdded) {
            updateUi(transactions)
        }
    }

    private fun loadData() {
        val transactions = preferences.getList(PreferencesKeys.ALL_TRANSACTIONS, Transaction.serializer())

        updateUi(transactions)
    }

    private var totalSpent = 0.0
    private var onFood = 0.0
    private var onAccommodation = 0.0

    private fun updateUi(transactions: List<Transaction>) {
        ParallelAsyncTasks(
            UpdateCanteensVisits(transactions) {
                if (isAdded && updateDataTask == null) {
                    updateCanteensVisitsUI(it)
                }
            },
            UpdateMostEatenMeals(transactions, activity::getString) {
                if (isAdded && updateDataTask == null) {
                    updateMostEatenMealsUI(it)
                }
            }
        ).executeAndThen {
            if (updateDataTask == null) {
                mView.swipeRefresh.isRefreshing = false
            }
        }

        ParallelAsyncTasks(
            FunAsyncTask(
                { TransactionsQueries.totalRecharges(transactions) },
                {
                    totalSpent = it
                    if (isAdded) {
                        mView.totalSpent.value.text = formatPrice(it)
                    }
                }
            ),
            FunAsyncTask(
                { -TransactionsQueries.totalFoodCost(transactions) },
                {
                    onFood = it
                    if (isAdded) {
                        mView.onFood.value.text = formatPrice(it)
                    }
                }
            ),
            FunAsyncTask(
                { -TransactionsQueries.totalAccommodationCost(transactions) },
                {
                    onAccommodation = it
                    if (isAdded) {
                        mView.onAccommodation.value.text = formatPrice(it)
                    }
                }
            )
        ).executeAndThen {
            if (isAdded) {
                mView.onOther.value.text = formatPrice(totalSpent - onFood - onAccommodation)
            }
        }
    }

    private fun formatPrice(totalSpent: Double): String {
        val price = if (totalSpent > -0.01 && totalSpent <= 0) {
            0.0
        } else {
            totalSpent
        }
        return String.format("%.2f €", price)
    }

    override fun canRefresh(): Boolean = true
}
