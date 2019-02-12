package sk.pluk64.unibakontoapp.asynctask

import android.os.AsyncTask

class ParallelAsyncTasks(private vararg val tasks: AsyncTaskWithCallbacks<*, *, *>) {

    init {
        tasks.forEach { task ->
            task.addCallback {
                onTaskDone(task)
            }
        }
    }

    private var doneTasks: MutableSet<AsyncTaskWithCallbacks<*, *, *>> = HashSet()
    private var then: () -> Unit = {}

    private fun onTaskDone(task: AsyncTaskWithCallbacks<*, *, *>) {
        doneTasks.add(task)
        if (doneTasks.size == tasks.size) {
            then()
        }
    }

    fun executeAndThen(f: () -> Unit) {
        then = f
        tasks.forEach {
            it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }
}