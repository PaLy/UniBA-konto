package sk.pluk64.unibakonto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FBUtils {
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    public static Date parseDate(String fbTime) {
        try {
            return dateFormatter.parse(fbTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
