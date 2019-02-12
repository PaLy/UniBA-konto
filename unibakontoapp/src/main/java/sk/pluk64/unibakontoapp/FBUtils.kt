package sk.pluk64.unibakontoapp

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FBUtils {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

    @Throws(ParseException::class)
    fun parseDate(fbTime: String): Date {
        try {
            return dateFormatter.parse(fbTime)
        } catch (e: RuntimeException) {
            System.err.println(fbTime)
            throw ParseException("", -1)
        }

    }

}
