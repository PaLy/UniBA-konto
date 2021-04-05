package sk.pluk64.unibakontoapp.meals

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakonto.Util
import java.io.IOException
import java.util.*

class UnibaDinnerServiceMealsProvider(canteen: Canteen) : MealsProvider {
    private val language: String = if (Locale.getDefault().language == "en") "en" else ""

    private val postData: Map<String, Any> = when (canteen) {
        Canteen.EAM -> mapOf("days" to 7, "facilities" to 44, "language" to language, "weekends" to "")
        Canteen.VENZA -> mapOf("days" to 7, "facilities" to 64, "language" to language, "weekends" to true)
    }

    override val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() {
            try {
                val postData = Util.paramsMap2PostData(this.postData)
                val urlConnection = UnibaKonto.httpPost("https://web-app1.uniba.sk/unibaMenu/ajax.php", postData)

                val stringResponse = Util.connInput2String(urlConnection)
                val response = Json.decodeFromString(JsonObject.serializer(), stringResponse)

                val mealsBuilder = Meals.builder()

                val status = response["status"]?.jsonPrimitive?.content
                if (status == "OK") {
                    val data = response["data"]
                    data?.jsonObject
                        ?.filterKeys { it != "id" }
                        ?.forEach { (date, submenus) ->
                            val day = submenus.jsonObject["day"]?.jsonPrimitive?.content.orEmpty()
                            mealsBuilder.newDay("$day $date")

                            submenus.jsonObject
                                .filterKeys { it != "day" }
                                .forEach { (submenuTitle, submenu) ->
                                    mealsBuilder.newSubMenu(submenuTitle)

                                    submenu.jsonObject.forEach { (mealName, meal) ->
                                        val getPrice = { key: String ->
                                            meal.jsonObject[key]?.jsonPrimitive?.content.orEmpty()
                                                .replace("&nbsp;", "")
                                        }
                                        val fullPrice = getPrice("FullPrice").replace("€", "")
                                        val clientPrice = getPrice("ClientPrice")

                                        val price = if (clientPrice.startsWith(fullPrice))
                                            clientPrice
                                        else
                                            "$fullPrice / $clientPrice"

                                        mealsBuilder.addMeal(Meals.Meal(mealName, price))
                                    }
                                }
                        }
                }

                return mealsBuilder.build()
            } catch (e: IOException) {
                throw Util.ConnectionFailedException()
            }
        }
}