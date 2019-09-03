package sk.pluk64.unibakontoapp.fragments.menu

import android.os.AsyncTask
import androidx.fragment.app.FragmentActivity

import com.facebook.FacebookException

import java.lang.ref.WeakReference

import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.R
import sk.pluk64.unibakontoapp.Utils
import sk.pluk64.unibakontoapp.meals.Canteen
import sk.pluk64.unibakontoapp.meals.Meals

internal class UpdateMenuDataTask(private val canteen: Canteen, activity: FragmentActivity, menuFragment: MenuFragment) : AsyncTask<Void, Void, Meals?>() {
    private val activityReference: WeakReference<FragmentActivity> = WeakReference(activity)
    private val menuFragmentReference: WeakReference<MenuFragment> = WeakReference(menuFragment)

    override fun doInBackground(vararg params: Void): Meals? {
        if (isCancelled) {
            return null
        }

        return try {
            canteen.menu
        } catch (e: Util.ConnectionFailedException) {
            val activity = activityReference.get()
            activity?.runOnUiThread { Utils.showNoInternetConnection(activity.applicationContext) }
            null
        }

    }

    override fun onPostExecute(meals: Meals?) {
        val menuFragment = menuFragmentReference.get()
        menuFragment?.onUpdateTaskFinished(meals)
    }

    override fun onCancelled(meals: Meals?) {
        val menuFragment = menuFragmentReference.get()
        menuFragment?.onUpdateTaskCancelled()
    }
}
