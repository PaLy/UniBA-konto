package sk.pluk64.unibakonto;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    private static Toast noInternetConnection;

    public static void showNoInternetConnection(Context context) {
        if (noInternetConnection == null || noInternetConnection.getView() == null || !noInternetConnection.getView().isShown()) {
            noInternetConnection = Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_LONG);
            noInternetConnection.show();
        }
    }

    public static void showToast(Context context, @StringRes int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
    }

    public static String getCurrentTimeFormatted() {
        return getTimeFormatted(getCurrentTime());
    }

    private static String getTimeFormatted(Date time) {
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

    private static final int MAX_LINK_LENGTH = 100;

    /**
     * By http://stackoverflow.com/questions/33203359/android-ellipsize-truncate-all-long-urls-in-a-textview
     */
    public static CharSequence shortenLinks(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Linkify.addLinks(builder, Linkify.ALL);
        URLSpan[] spans = builder.getSpans(0, builder.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            int flags = builder.getSpanFlags(span);

            CharSequence linkText = builder.subSequence(start, end);
            if (linkText.length() > MAX_LINK_LENGTH) {

                // 1 - Remove the https:// or http:// prefix
                if (linkText.toString().toLowerCase().startsWith("https://"))
                    linkText = linkText.subSequence("https://".length(), linkText.length());
                else if (linkText.toString().toLowerCase().startsWith("http://"))
                    linkText = linkText.subSequence("http://".length(), linkText.length());

                // 2 - Remove the www. prefix
                if (linkText.toString().toLowerCase().startsWith("www."))
                    linkText = linkText.subSequence("www.".length(), linkText.length());

                // 3 - Truncate if still longer than MAX_LINK_LENGTH
                if (linkText.length() > MAX_LINK_LENGTH) {
                    linkText = linkText.subSequence(0, MAX_LINK_LENGTH) + "…";
                }

                // 4 - Replace the text preserving the spans
                builder.replace(start, end, linkText);
                builder.removeSpan(span);
                builder.setSpan(span, start, start + linkText.length(), flags);
            }
        }
        return builder;
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

    private static float getLineHeight(TextView textView) {
        return textView.getPaint().getFontMetrics().bottom - textView.getPaint().getFontMetrics().top;
    }

    public static int getViewHeightFromTextHeight(TextView textView) {
        int lineCount = textView.getLineCount();
        float lineHeight = getLineHeight(textView);
        return (int) Math.ceil(lineCount * lineHeight);
    }

    public static class FBAuthenticationException extends Exception {
    }
}
