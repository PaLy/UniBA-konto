package sk.pluk64.unibakontoapp;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static String getCurrentTimeFormatted() {
        return getTimeFormatted(getCurrentTime());
    }

    private static String getTimeFormatted(Date time) {
        return getTimeFormatted(time, "");
    }

    public static String getTimeFormatted(Date time, String defaultIfNull) {
        if (time == null) {
            return defaultIfNull;
        } else {
            return new SimpleDateFormat("d.M.yyyy HH:mm").format(time);
        }
    }

    public static String getReadableTime(Date time, String defaultIfNull, Context context) {
        if (time == null) {
            return defaultIfNull;
        } else if (isToday(time)) {
            return new SimpleDateFormat("HH:mm").format(time);
        } else if (isYesterday(time)) {
            return context.getResources().getString(R.string.yesterday) + " " + new SimpleDateFormat("HH:mm").format(time);
        } else {
            return new SimpleDateFormat("d.M. HH:mm").format(time);
        }
    }

    public static Date getCurrentTime() {
        return Calendar.getInstance().getTime();
    }

    public static boolean isTimeDiffMoreThanXHours(Date time, int x) {
        if (time == null) {
            return true;
        } else {
            long timeDiff = getCurrentTime().getTime() - time.getTime();
            long timeDiffHours = timeDiff / (1000 * 60 * 60);
            return timeDiffHours >= x;
        }
    }

    public static boolean isToday(Date time) {
        if (time == null) {
            return false;
        } else {
            Calendar nowTime = Calendar.getInstance();

            Calendar thenTime = Calendar.getInstance();
            thenTime.setTime(time);

            return nowTime.get(Calendar.DAY_OF_YEAR) == thenTime.get(Calendar.DAY_OF_YEAR) &&
                nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR);
        }
    }

    public static boolean isYesterday(Date time) {
        if (time == null) {
            return false;
        } else {
            Calendar nowTime = Calendar.getInstance();

            Calendar thenTime = Calendar.getInstance();
            thenTime.setTime(time);
            thenTime.add(Calendar.DAY_OF_YEAR, 1);

            return nowTime.get(Calendar.DAY_OF_YEAR) == thenTime.get(Calendar.DAY_OF_YEAR) &&
                nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR);
        }
    }

    public static boolean isAtMostXHoursOld(Date time, long x) {
        if (time == null) {
            return false;
        } else {
            long nowTime = System.currentTimeMillis();
            long hoursDiff = (nowTime - time.getTime()) / (1000 * 60 * 60);

            return hoursDiff < x;
        }
    }
}
