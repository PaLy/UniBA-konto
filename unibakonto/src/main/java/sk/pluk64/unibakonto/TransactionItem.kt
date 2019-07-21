package sk.pluk64.unibakonto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat

@Serializable
data class TransactionItem(
    val timestamp: String = "",
    val service: String = "",
    val shortcut: String = "",
    val description: String = "",
    val amount: String = "",
    val method: String = "",
    val obj: String = "",
    val payed: String = ""
) {
    @Transient
    val parsedTimestamp by lazy {
        try {
            DATE_FORMAT.parse(timestamp)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    @Transient
    val parsedAmount by lazy {
        try {
            amount.replace(',', '.').toDouble()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        fun fromElement(tableRow: Element): TransactionItem {
            val columns = tableRow.children().iterator()

            val timestamp = columns.next().text()
            val service = columns.next().text()
            val shortcut = columns.next().text()
            val description = columns.next().text()
            val amount = columns.next().text()
            val method = columns.next().text()
            val obj = columns.next().text()
            val payed = columns.next().text()

            return TransactionItem(timestamp, service, shortcut, description, amount, method, obj, payed)
        }

        private val DATE_FORMAT
            get() = SimpleDateFormat("d. MM. yyyy HH:mm:ss")
    }
}
