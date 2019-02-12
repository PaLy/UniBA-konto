package sk.pluk64.unibakontoapp.asynctask

class FunAsyncTask<T>(
    private val task: () -> T,
    resultCallback: (T) -> Unit) : AsyncTaskWithCallbacks<Void, Void, T>() {

    init {
        addCallback(resultCallback)
    }

    private var then: () -> Unit = {}

    override fun doInBackground(vararg params: Void): T {
        return task()
    }

    override fun onPostExecute(result: T) {
        super.onPostExecute(result)
        then()
    }

    fun executeAndThen(f: () -> Unit) {
        then = f
        executeOnExecutor(THREAD_POOL_EXECUTOR)
    }
}
