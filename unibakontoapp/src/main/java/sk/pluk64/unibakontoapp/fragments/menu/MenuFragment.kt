package sk.pluk64.unibakontoapp.fragments.menu

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_menu.view.*
import kotlinx.serialization.json.Json
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.meals.Canteen
import sk.pluk64.unibakontoapp.meals.Meals
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import sk.pluk64.unibakontoapp.preferencesutils.getDate
import sk.pluk64.unibakontoapp.preferencesutils.getMeals
import java.util.*

class MenuFragment : Fragment(), Refreshable {
    lateinit var activity: MainActivity
        private set
    private lateinit var mView: View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    private val preferences: SharedPreferences by lazy {
        activity.getPreferences(Context.MODE_PRIVATE)
    }

    private lateinit var canteen: Canteen
    private val adapter = MenuListAdapter(this)
    private var updateDataTask: UpdateMenuDataTask? = null
    private var refreshTime: Date? = null
    private var refreshMenusListener: RefreshMenusListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canteen = arguments?.getSerializable(ARG_CANTEEN) as Canteen
        refreshMenusListener?.let {
            adapter.setRefreshMenusListener(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateDataTask?.cancel(true)
    }

    override fun refresh() {
        if (updateDataTask != null) {
            return
        }

        adapter.setRefreshing()
        val updateDataTask = UpdateMenuDataTask(canteen, activity, this)
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        this.updateDataTask = updateDataTask
    }

    override fun canRefresh(): Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        mView = view

        mView.menu_list.setHasFixedSize(true)
        val tLayoutManager = LinearLayoutManager(context)
        mView.menu_list.layoutManager = tLayoutManager
        mView.menu_list.adapter = adapter

        loadData()
        refreshTime?.let { adapter.updateRefreshTime(it) }

        mView.swipe_refresh.setOnRefreshListener { refresh() }
        return view
    }

    override fun onResume() {
        super.onResume()
        if (DateUtils.isTimeDiffMoreThanXHours(refreshTime, 1)) {
            mView.swipe_refresh.post { refresh() }
        }
    }

    private fun saveData(meals: Meals) {
        val jsonMeals = Json.stringify(Meals.serializer(), meals)
        refreshTime = DateUtils.currentTime
        val jsonCurrentTime = Json.stringify(DateSerializer, refreshTime)

        preferences.edit()
            .putString(PreferencesKeys.meals(canteen), jsonMeals)
            .putString(PreferencesKeys.mealsRefreshTimestamp(canteen), jsonCurrentTime)
            .apply()
    }

    private fun loadData() {
        val meals = preferences.getMeals(PreferencesKeys.meals(canteen))
        adapter.updateMeals(meals)

        refreshTime = preferences.getDate(PreferencesKeys.mealsRefreshTimestamp(canteen))
    }

    fun onUpdateTaskFinished(meals: Meals?) {

        meals?.let {
            saveData(it)
            adapter.updateMeals(it)
        }

        refreshTime?.let { adapter.updateRefreshTime(it) }
        mView.swipe_refresh.isRefreshing = false
        updateDataTask = null
    }

    fun onUpdateTaskCancelled() {
        updateDataTask = null
    }

    companion object {
        private const val ARG_CANTEEN = "jedalen"

        fun newInstance(canteen: Canteen, refreshMenusListener: RefreshMenusListener): MenuFragment {
            val f = MenuFragment()

            val args = Bundle()
            args.putSerializable(ARG_CANTEEN, canteen)
            f.arguments = args

            f.refreshMenusListener = refreshMenusListener

            return f
        }
    }
}
