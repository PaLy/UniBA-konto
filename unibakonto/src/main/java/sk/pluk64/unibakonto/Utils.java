package sk.pluk64.unibakonto;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Utils {
    private static Toast noInternetConnection;

    public static void showNoInternetConnection(Context context) {
        if (noInternetConnection == null || noInternetConnection.getView() == null || !noInternetConnection.getView().isShown()) {
            noInternetConnection = Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_LONG);
            noInternetConnection.show();
        }
    }

    public static String getCurrentTimeFormatted() {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat("d.M.yyyy HH:mm").format(calendar.getTime());
    }
}
