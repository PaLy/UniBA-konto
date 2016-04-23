package sk.pluk64.unibakonto;

import android.content.Context;
import android.widget.Toast;

public class Toasts {
    private static Toast noInternetConnection;

    public static void showNoInternetConnection(Context context) {
        if (noInternetConnection == null || noInternetConnection.getView() == null || !noInternetConnection.getView().isShown()) {
            noInternetConnection = Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_LONG);
            noInternetConnection.show();
        }
    }
}
