package sk.pluk64.unibakonto;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    private static Toast noInternetConnection;

    public static void showNoInternetConnection(Context context) {
        if (noInternetConnection == null || noInternetConnection.getView() == null || !noInternetConnection.getView().isShown()) {
            noInternetConnection = Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_LONG);
            noInternetConnection.show();
        }
    }

    public static String getCurrentTimeFormatted() {
        return getTimeFormatted(getCurrentTime());
    }

    public static String getTimeFormatted(Date time) {
        return getTimeFormatted(time, null);
    }

    public static String getTimeFormatted(Date time, String def) {
        if (time == null) {
            return def;
        } else {
            return new SimpleDateFormat("d.M.yyyy HH:mm").format(time);
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

    public static String getFirstOrEmpty(Elements elements) {
        if (elements.size() > 0) {
            return elements.get(0).text().trim();
        } else {
            return "";
        }
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int dpToPx(int dp) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
