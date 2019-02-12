package sk.pluk64.unibakontoapp.asynctask

import android.os.AsyncTask

abstract class AsyncTaskWithCallbacks<Params, Progress, Result> : AsyncTask<Params, Progress, Result>() {

    private val callbacks: MutableList<(Result) -> Unit> = ArrayList()

    fun addCallback(callback: (Result) -> Unit) = callbacks.add(callback)

    override fun onPostExecute(result: Result) {
        super.onPostExecute(result)
        callbacks.forEach { it(result) }
    }
}
