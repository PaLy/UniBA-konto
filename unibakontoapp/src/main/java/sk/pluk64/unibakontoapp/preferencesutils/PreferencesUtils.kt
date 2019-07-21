package sk.pluk64.unibakontoapp.preferencesutils

import android.content.SharedPreferences
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import sk.pluk64.unibakonto.Balances
import sk.pluk64.unibakontoapp.meals.Meals
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date?> {
    private val dateFormat: DateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

    override val descriptor: SerialDescriptor = StringDescriptor.withName("Date")

    override fun serialize(encoder: Encoder, obj: Date?) {
        return encoder.encodeString(
            if (obj != null) {
                dateFormat.format(obj)
            } else {
                ""
            }
        )
    }

    override fun deserialize(decoder: Decoder): Date? {
        return dateFormat.parse(decoder.decodeString())
    }
}

private fun <T> SharedPreferences.getOr(prefKey: String, serializer: KSerializer<T>, defaultValue: T): T {
    val json = getString(prefKey, "") ?: ""
    return if (json != "") {
        Json.parse(serializer, json)
    } else {
        defaultValue
    }
}

fun SharedPreferences.getDate(prefKey: String) = getOr(prefKey, DateSerializer, null)

fun <T> SharedPreferences.getList(prefKey: String, serializer: KSerializer<T>) =
    getOr(prefKey, serializer.list, emptyList())

fun SharedPreferences.getBalances(prefKey: String) =
    getOr(prefKey, Balances.serializer(), Balances.EMPTY)

fun SharedPreferences.getMeals(prefKey: String) = getOr(prefKey, Meals.serializer(), Meals.EMPTY)
