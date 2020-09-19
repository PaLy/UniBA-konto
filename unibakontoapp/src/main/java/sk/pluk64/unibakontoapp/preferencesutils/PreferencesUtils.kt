package sk.pluk64.unibakontoapp.preferencesutils

import android.content.SharedPreferences
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date?) {
        return encoder.encodeString(
            if (value != null) {
                dateFormat.format(value)
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
        Json.decodeFromString(serializer, json)
    } else {
        defaultValue
    }
}

fun SharedPreferences.getDate(prefKey: String) = getOr(prefKey, DateSerializer, null)

fun <T> SharedPreferences.getList(prefKey: String, serializer: KSerializer<T>) =
    getOr(prefKey, ListSerializer(serializer), emptyList())

fun SharedPreferences.getBalances(prefKey: String) =
    getOr(prefKey, Balances.serializer(), Balances.EMPTY)

fun SharedPreferences.getMeals(prefKey: String) = getOr(prefKey, Meals.serializer(), Meals.EMPTY)
