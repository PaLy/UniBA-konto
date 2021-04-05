package sk.pluk64.unibakontoapp.fragments.menu

import android.content.Intent
import android.net.Uri
import android.util.SparseArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import kotlinx.android.synthetic.main.fb_images.view.*
import kotlinx.android.synthetic.main.menu_item_meal.view.*
import kotlinx.android.synthetic.main.social_media_links.view.*
import sk.pluk64.unibakontoapp.*
import sk.pluk64.unibakontoapp.meals.Meals
import java.util.*

internal class MenuListAdapter(private val menuFragment: MenuFragment) : RecyclerView.Adapter<MenuListAdapter.ViewHolder>() {
    private val menuImagesAdapter: MenuImagesAdapter = MenuImagesAdapter(menuFragment)
    private var refreshMenusListener: RefreshMenusListener? = null

    private var itemCount = 0
    private val positionToItem = SparseArray<Any>()
    private val positionToViewType = SparseArray<ViewType>()

    private enum class ViewType(internal val id: Int) {
        DAY_NAME(0), SUBMENU_NAME(1), MEAL(2), GALLERY(3), NO_GALLERY_IMAGES(5), REFRESH_TIMESTAMP(6),
        GALLERY_LOADING(7), SOCIAL_MEDIA_LINKS(8)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    init {
        REFRESH_TIMESTAMP_POSITION.let {
            positionToViewType.put(it, ViewType.REFRESH_TIMESTAMP)
            positionToItem.put(it, "")
        }
        SOCIAL_MEDIA_LINKS_POSITION.let {
            positionToViewType.put(it, ViewType.SOCIAL_MEDIA_LINKS)
            positionToItem.put(it, "")
        }
        itemCount = 2
    }

    private fun isSocNetworksHidden() = menuFragment.preferences.getBoolean(PreferencesKeys.SOC_NETWORKS_HIDDEN, false)

    fun setRefreshMenusListener(refreshMenusListener: RefreshMenusListener) {
        this.refreshMenusListener = refreshMenusListener
    }

    fun updatePhotos(photos: List<FBPhoto>) {
        if (photos.isEmpty()) {
            positionToViewType.put(FB_FEED_POSITION, ViewType.NO_GALLERY_IMAGES)
        } else {
            positionToViewType.put(FB_FEED_POSITION, ViewType.GALLERY)
        }
        menuImagesAdapter.updateData(photos)
        notifyDataSetChanged()
    }

    fun updateMeals(meals: Meals?) {
        var pos = 2
        if (meals != null) {
            for (dayMenu in meals.menus) {
                positionToItem.put(pos, dayMenu.dayName)
                positionToViewType.put(pos, ViewType.DAY_NAME)
                pos++
                for (subMenu in dayMenu.subMenus) {
                    positionToItem.put(pos, subMenu.name)
                    positionToViewType.put(pos, ViewType.SUBMENU_NAME)
                    pos++
                    for (meal in subMenu.meals) {
                        positionToItem.put(pos, meal)
                        positionToViewType.put(pos, ViewType.MEAL)
                        pos++
                    }
                }
            }
        }
        itemCount = pos
        notifyDataSetChanged()
    }

    fun setRefreshing() {
        positionToItem.put(REFRESH_TIMESTAMP_POSITION, menuFragment.getString(R.string.refreshing))
        notifyDataSetChanged()
    }

    fun updateRefreshTime(refreshTime: Date) {
        val refreshTimeFormatted = DateUtils.getReadableTime(refreshTime, menuFragment.getString(R.string.never), menuFragment.context!!)
        positionToItem.put(REFRESH_TIMESTAMP_POSITION, menuFragment.getString(R.string.refreshed, refreshTimeFormatted))
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = positionToViewType.get(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ViewType.DAY_NAME.id -> LayoutInflater.from(parent.context).inflate(R.layout.menu_item_day, parent, false)
            ViewType.SUBMENU_NAME.id -> LayoutInflater.from(parent.context).inflate(R.layout.menu_item_submenu, parent, false)
            ViewType.MEAL.id -> LayoutInflater.from(parent.context).inflate(R.layout.menu_item_meal, parent, false)
            ViewType.GALLERY.id -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.fb_images, parent, false)

                view.recycler.setHasFixedSize(false)
                val layoutManager = GridLayoutManager(parent.context, 1, LinearLayoutManager.HORIZONTAL, false)
                view.recycler.layoutManager = layoutManager
                view.recycler.adapter = menuImagesAdapter

                val snapHelper = GravitySnapHelper(Gravity.START)
                snapHelper.attachToRecyclerView(view.recycler)
                view
            }
            ViewType.NO_GALLERY_IMAGES.id -> LayoutInflater.from(parent.context).inflate(R.layout.no_fb_images, parent, false)
            ViewType.GALLERY_LOADING.id -> View(parent.context) // empty
            ViewType.REFRESH_TIMESTAMP.id -> LayoutInflater.from(parent.context).inflate(R.layout.refreshed_timestamp_menu, parent, false)
            ViewType.SOCIAL_MEDIA_LINKS.id -> LayoutInflater.from(parent.context).inflate(R.layout.social_media_links, parent, false)
            else -> throw Throwable()
        }.run { ViewHolder(this) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (positionToViewType.get(position)) {
            ViewType.DAY_NAME -> {
                val view = holder.view as TextView
                view.text = positionToItem.get(position) as CharSequence
            }
            ViewType.SUBMENU_NAME -> {
                val view = holder.view as TextView
                view.text = positionToItem.get(position) as CharSequence
            }
            ViewType.MEAL -> {
                val view = holder.view
                val meal = positionToItem.get(position) as Meals.Meal

                view.meal_name.text = meal.name
                view.meal_cost.text = meal.price
            }
            ViewType.REFRESH_TIMESTAMP -> {
                val view = holder.view as TextView
                view.text = positionToItem.get(position) as CharSequence
            }
            ViewType.SOCIAL_MEDIA_LINKS -> {
                if (isSocNetworksHidden()) {
                    holder.view.visibility = View.GONE
                    holder.view.layoutParams = RecyclerView.LayoutParams(0, 0)
                } else {
                    holder.view.visibility = View.VISIBLE
                    holder.view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                    val fbIcon = holder.view.fb_icon
                    fbIcon.setOnClickListener {
                        openLink(menuFragment.canteen.fbUri)
                    }
                    val igIcon = holder.view.ig_icon
                    igIcon.setOnClickListener {
                        openLink(menuFragment.canteen.igUri)
                    }
                }
            }
            ViewType.GALLERY -> return
            ViewType.NO_GALLERY_IMAGES -> return
            ViewType.GALLERY_LOADING -> return
            null -> return
        }
    }


    private fun openLink(uri: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        menuFragment.activity.startActivity(browserIntent)
    }

    override fun getItemCount() = itemCount

    companion object {
        private const val REFRESH_TIMESTAMP_POSITION = 0
        private const val SOCIAL_MEDIA_LINKS_POSITION = 1
        private const val FB_FEED_POSITION = 2
    }
}
