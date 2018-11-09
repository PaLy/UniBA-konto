package sk.pluk64.unibakontoapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FBUtils {
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    public static Date parseDate(String fbTime) throws ParseException {
        try {
            return dateFormatter.parse(fbTime);
        } catch (RuntimeException e) {
            throw new ParseException("", -1);
        }
    }

}
