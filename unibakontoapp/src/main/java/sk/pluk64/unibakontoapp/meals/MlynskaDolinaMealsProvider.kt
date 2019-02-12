package sk.pluk64.unibakontoapp.meals

import org.jsoup.Jsoup
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakonto.Util
import java.io.IOException

class MlynskaDolinaMealsProvider(canteen: Canteen) : MealsProvider {
    private val location: String
    private val someParam: Int

    override
    val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() {
            try {
                val urlConnection = UnibaKonto.httpGet(location)
                val doc = Jsoup.parse(Util.connInput2String(urlConnection))
                val elements = doc.select(".field-content")

                val mealsBuilder = Meals.builder()
                for (e in elements[someParam].children()) {
                    if (e.tagName() == "h2") {
                        mealsBuilder.newDay(e.select("a").text())
                    } else if (e.tagName() == "p") {
                        val strong = e.select("strong")
                        if (!strong.isEmpty()) {
                            mealsBuilder.newSubMenu(strong[0].text())
                        }
                    } else if (e.tagName() == "table") {
                        val meals = e.select("tr")
                        for (meal in meals) {
                            val cols = meal.select("td")
                            val name = cols[0].text().replace("\u00a0", "")
                            var cost = cols[1].text().replace("\u00a0", "")
                            val n = cost.length
                            cost = cost.substring(n - 6, n - 1)
                            mealsBuilder.addMeal(Meals.Meal(name, cost))
                        }
                    }
                }

                return mealsBuilder.build()
            } catch (e: IOException) {
                throw Util.ConnectionFailedException()
            }

        }

    init {
        when (canteen) {
            Canteen.EAM -> {
                location = "http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/eat-meet/denne-menu"
                someParam = 4
            }
            Canteen.VENZA -> {
                location = "http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/venza/denne-menu"
                someParam = 3
            }
        }
    }
}
