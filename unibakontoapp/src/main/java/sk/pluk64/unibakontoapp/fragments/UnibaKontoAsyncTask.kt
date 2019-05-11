package sk.pluk64.unibakontoapp.fragments

import android.os.AsyncTask
import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.Utils
import java.lang.ref.WeakReference

abstract class UnibaKontoAsyncTask<T>(activity: MainActivity) : AsyncTask<Void, Void, T?>() {
    private val activity: WeakReference<MainActivity> = WeakReference(activity)
    private var failed = false

    private fun showNoInternetConnectionToastFromBackgroundThread() {
        activity.get()?.let {
            it.runOnUiThread { Utils.showNoInternetConnection(it.applicationContext) }
        }
    }

    public override fun doInBackground(vararg params: Void): T? {
        return activity.get()?.let {
            val unibaKonto = it.unibaKonto
            if (!unibaKonto.isLoggedIn(true)) {
                try {
                    unibaKonto.login()
                } catch (e: Util.ConnectionFailedException) {
                    showNoInternetConnectionToastFromBackgroundThread()
                }

            }
            return if (unibaKonto.isLoggedIn) {
                try {
                    load()
                } catch (e: Util.ConnectionFailedException) {
                    showNoInternetConnectionToastFromBackgroundThread()
                    failed = true
                    null
                }
            } else {
                failed = true
                null
            }
        }
    }

    abstract fun load():T?

    override fun onPostExecute(result: T?) {
        onFinish(result)
    }

    override fun onCancelled(result: T?) {
        onFinish(result)
    }

    abstract fun onFinish(result: T?)
}
