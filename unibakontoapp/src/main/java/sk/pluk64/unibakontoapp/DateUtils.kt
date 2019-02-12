package sk.pluk64.unibakontoapp

import android.content.Context

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object DateUtils {
    val currentTimeFormatted: String
        get() = getTimeFormatted(currentTime)

    val currentTime: Date
        get() = Calendar.getInstance().time

    private fun getTimeFormatted(time: Date): String {
        return getTimeFormatted(time, "")
    }

    private fun getTimeFormatted(time: Date?, defaultIfNull: String): String {
        return if (time == null) {
            defaultIfNull
        } else {
            SimpleDateFormat("d.M.yyyy HH:mm").format(time)
        }
    }

    fun getReadableTime(time: Date?, defaultIfNull: String, context: Context): String {
        return when {
            time == null -> defaultIfNull
            isToday(time) -> SimpleDateFormat("HH:mm").format(time)
            isYesterday(time) -> context.resources.getString(R.string.yesterday) + " " + SimpleDateFormat("HH:mm").format(time)
            else -> SimpleDateFormat("d.M. HH:mm").format(time)
        }
    }

    fun isTimeDiffMoreThanXHours(time: Date?, x: Int): Boolean {
        return if (time == null) {
            true
        } else {
            val timeDiff = currentTime.time - time.time
            val timeDiffHours = timeDiff / (1000 * 60 * 60)
            timeDiffHours >= x
        }
    }

    fun isToday(time: Date?): Boolean {
        return if (time == null) {
            false
        } else {
            val nowTime = Calendar.getInstance()

            val thenTime = Calendar.getInstance()
            thenTime.time = time

            nowTime.get(Calendar.DAY_OF_YEAR) == thenTime.get(Calendar.DAY_OF_YEAR)
                && nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR)
        }
    }

    fun isYesterday(time: Date?): Boolean {
        return if (time == null) {
            false
        } else {
            val nowTime = Calendar.getInstance()

            val thenTime = Calendar.getInstance()
            thenTime.time = time
            thenTime.add(Calendar.DAY_OF_YEAR, 1)

            nowTime.get(Calendar.DAY_OF_YEAR) == thenTime.get(Calendar.DAY_OF_YEAR) && nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR)
        }
    }

    fun isAtMostXHoursOld(time: Date?, x: Long): Boolean {
        return if (time == null) {
            false
        } else {
            val nowTime = System.currentTimeMillis()
            val hoursDiff = (nowTime - time.time) / (1000 * 60 * 60)

            hoursDiff < x
        }
    }

    fun notThisMonth(time: Date?): Boolean {
        return if (time == null) {
            true
        } else {
            val nowTime = Calendar.getInstance()

            val thenTime = Calendar.getInstance()
            thenTime.time = time

            nowTime.get(Calendar.MONTH) != thenTime.get(Calendar.MONTH)
                || nowTime.get(Calendar.YEAR) != thenTime.get(Calendar.YEAR)
        }
    }
}
