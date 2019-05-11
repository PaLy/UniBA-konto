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
    private var photos: List<FBPhoto> = emptyList()
    private var needAuthenticate = false

    override fun doInBackground(vararg params: Void): Meals? {
        // TODO otestovat, co sa stane ak je FB nedostupny.
        // jedalne listky by sa mali stiahnut aj bez FB

        try {
            photos = FBPageFeedFoodPhotosSupplier(canteen).photos
            photos = if (photos.isEmpty()) {
                FBPageUploadedImagesFoodPhotosSupplier(canteen).photos
            } else {
                HigherResolutionFbPhotosSupplier(canteen, photos).photos
            }
        } catch (e: FacebookException) {
            val activity = activityReference.get()
            activity?.runOnUiThread { Utils.showToast(activity.applicationContext, R.string.internal_error) }
        } catch (e: Utils.FBAuthenticationException) {
            needAuthenticate = true
        } catch (e: Util.ConnectionFailedException) {
            val activity = activityReference.get()
            activity?.runOnUiThread { Utils.showNoInternetConnection(activity.applicationContext) }
        }

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
        menuFragment?.onUpdateTaskFinished(needAuthenticate, meals, photos)
    }

    override fun onCancelled(meals: Meals?) {
        val menuFragment = menuFragmentReference.get()
        menuFragment?.onUpdateTaskCancelled()
    }
}
