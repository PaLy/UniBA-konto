package sk.pluk64.unibakonto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FBUtils {
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    public static Date parseDate(String fbTime) {
        try {
            return dateFormatter.parse(fbTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
