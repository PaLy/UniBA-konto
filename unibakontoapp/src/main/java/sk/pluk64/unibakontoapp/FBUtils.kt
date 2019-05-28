package sk.pluk64.unibakontoapp

import com.facebook.AccessToken
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FBUtils {
    // SimpleDateFormat is not thread-safe!
    // Always new instance will fix it
    private val dateFormatter
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

    @Throws(ParseException::class)
    fun parseDate(fbTime: String): Date {
        try {
            val date = dateFormatter.parse(fbTime)
            if (date != null) {
                return date
            } else {
                System.err.println(fbTime)
                throw ParseException("", -1)
            }
        } catch (e: RuntimeException) {
            System.err.println(fbTime)
            throw ParseException("", -1)
        }

    }

    fun isLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }
}
