package sk.pluk64.unibakontoapp

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.DisplayMetrics
import android.widget.TextView
import android.widget.Toast
import org.jsoup.select.Elements

object Utils {

    val screenWidth: Int
        @JvmStatic
        get() = Resources.getSystem().displayMetrics.widthPixels

    private const val MAX_LINK_LENGTH = 100

    @JvmStatic
    fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    private var noInternetConnection: Toast? = null

    @JvmStatic
    fun showNoInternetConnection(context: Context) {
        if (noInternetConnection == null || noInternetConnection!!.view == null || !noInternetConnection!!.view.isShown) {
            noInternetConnection = Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_LONG)
            noInternetConnection!!.show()
        }
    }

    @JvmStatic
    fun showToast(context: Context, @StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    @JvmStatic
    fun getFirstOrEmpty(elements: Elements): String {
        return if (elements.size > 0) {
            elements[0].text().trim { it <= ' ' }
        } else {
            ""
        }
    }

    @JvmStatic
    fun dpToPx(dp: Int): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    /**
     * By http://stackoverflow.com/questions/33203359/android-ellipsize-truncate-all-long-urls-in-a-textview
     */
    @JvmStatic
    fun shortenLinks(text: String): CharSequence {
        val builder = SpannableStringBuilder(text)
        Linkify.addLinks(builder, Linkify.ALL)
        val spans = builder.getSpans(0, builder.length, URLSpan::class.java)
        for (span in spans) {
            val start = builder.getSpanStart(span)
            val end = builder.getSpanEnd(span)
            val flags = builder.getSpanFlags(span)

            var linkText = builder.subSequence(start, end)
            if (linkText.length > MAX_LINK_LENGTH) {

                // 1 - Remove the https:// or http:// prefix
                if (linkText.toString().toLowerCase().startsWith("https://"))
                    linkText = linkText.subSequence("https://".length, linkText.length)
                else if (linkText.toString().toLowerCase().startsWith("http://"))
                    linkText = linkText.subSequence("http://".length, linkText.length)

                // 2 - Remove the www. prefix
                if (linkText.toString().toLowerCase().startsWith("www."))
                    linkText = linkText.subSequence("www.".length, linkText.length)

                // 3 - Truncate if still longer than MAX_LINK_LENGTH
                if (linkText.length > MAX_LINK_LENGTH) {
                    linkText = linkText.subSequence(0, MAX_LINK_LENGTH).toString() + "…"
                }

                // 4 - Replace the text preserving the spans
                builder.replace(start, end, linkText)
                builder.removeSpan(span)
                builder.setSpan(span, start, start + linkText.length, flags)
            }
        }
        return builder
    }

    private fun getLineHeight(textView: TextView): Float {
        return textView.paint.fontMetrics.bottom - textView.paint.fontMetrics.top
    }

    @JvmStatic
    fun getViewHeightFromTextHeight(textView: TextView): Int {
        val lineCount = textView.lineCount
        val lineHeight = getLineHeight(textView)
        return Math.ceil((lineCount * lineHeight).toDouble()).toInt()
    }

    class FBAuthenticationException : Exception()
}
