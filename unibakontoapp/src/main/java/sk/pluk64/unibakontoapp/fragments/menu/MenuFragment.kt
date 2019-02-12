package sk.pluk64.unibakontoapp.fragments.menu

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.CallbackManager
import kotlinx.android.synthetic.main.fragment_menu.view.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.meals.Canteen
import sk.pluk64.unibakontoapp.meals.Meals
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import sk.pluk64.unibakontoapp.preferencesutils.getDate
import sk.pluk64.unibakontoapp.preferencesutils.getList
import sk.pluk64.unibakontoapp.preferencesutils.getMeals
import java.util.*

class MenuFragment : Fragment(), Refreshable {
    lateinit var activity: MainActivity
        private set
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

    private lateinit var canteen: Canteen
    private val adapter = MenuListAdapter(this)
    private var updateDataTask: AsyncTask<Void, Void, Meals>? = null
    private var refreshTime: Date? = null
    val fbCallbackManager by lazy {
        CallbackManager.Factory.create()
    }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fbCallbackManager.onActivityResult(requestCode, resultCode, data)
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

        val photos = preferences.getList(PreferencesKeys.mealsPhotos(canteen), FBPhoto.serializer())
        adapter.updatePhotos(photos)

        refreshTime = preferences.getDate(PreferencesKeys.mealsRefreshTimestamp(canteen))
    }

    private fun saveData(photosWithSources: List<FBPhoto>) {
        val jsonPhotos = Json.stringify(FBPhoto.serializer().list, photosWithSources)
        preferences.edit()
            .putString(PreferencesKeys.mealsPhotos(canteen), jsonPhotos)
            .apply()
    }

    fun onUpdateTaskFinished(needAuthenticate: Boolean, meals: Meals?, photos: List<FBPhoto>) {
        if (needAuthenticate) {
            saveData(photos)
            adapter.showFBButton()
        } else {
            saveData(photos)
            adapter.updatePhotos(photos)
        }

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
