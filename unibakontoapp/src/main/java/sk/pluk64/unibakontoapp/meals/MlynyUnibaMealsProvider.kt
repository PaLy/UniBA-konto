package sk.pluk64.unibakontoapp.meals

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.Utils
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MlynyUnibaMealsProvider(canteen: Canteen) : MealsProvider {
    private val location: String = when (canteen) {
        Canteen.EAM -> "https://mlyny.uniba.sk/stravovanie/eat-and-meet/denne-menu-eat-and-meet/"
        Canteen.VENZA -> "https://mlyny.uniba.sk/stravovanie/venza/denne-menu-venza/"
    }

    override val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() {
            try {
                val urlConnection = UnibaKonto.httpGet(location)
                val doc = Jsoup.parse(Util.connInput2String(urlConnection))
                val mealsBuilder = Meals.builder()

                val days = doc.select(".card")

                for (day in days) {
                    parseDay(mealsBuilder, day)
                }

                return mealsBuilder.build()
            } catch (e: IOException) {
                throw Util.ConnectionFailedException()
            }

        }

    private fun parseDay(mealsBuilder: Meals.Builder, day: Element) {
        val dayTitle = Utils.getFirstOrEmpty(day.select(".mdm_menu_day_name"))

        if (!isOldDay(dayTitle)) {
            mealsBuilder.newDay(dayTitle)

            val menuTable = day.selectFirst("table")
            if (menuTable != null) {
                parseDayMenu(mealsBuilder, menuTable)
            }
        }
    }

    private fun isOldDay(dayTitle: String): Boolean {
        val dayParts = dayTitle.trim().split(" ")
        if (dayParts.size > 1) {
            val date = dayParts[1]
            try {
                val today = Calendar.getInstance()

                val d = dateFormatter.parse(date)
                val cDate = Calendar.getInstance()
                cDate.time = d!!

                return today.get(Calendar.YEAR) > cDate.get(Calendar.YEAR) || today.get(Calendar.YEAR) == cDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) > cDate.get(Calendar.DAY_OF_YEAR)
            } catch (ignored: ParseException) {
            }
        }
        return false
    }

    private fun parseDayMenu(mealsBuilder: Meals.Builder, menu: Element) {
        val allRows = menu.select("tr")
        if (allRows.size > 1) {
            val rows = allRows.subList(1, allRows.size)
            for (row in rows) {
                val tds = row.select("td")
                if (tds.size == 1) {
                    val subMenuTitle = Utils.getFirstOrEmpty(tds)
                    mealsBuilder.newSubMenu(subMenuTitle)
                } else {
                    parseMeal(mealsBuilder, row)
                }
            }
        }
    }

    private fun parseMeal(mealsBuilder: Meals.Builder, meal: Element) {
        val tds = meal.select("td")
        val mealName = Utils.getFirstOrEmpty(tds)

        val mainPrice = if (tds.size > 1) tds[1].text().trim() else ""
        val studentPrice = if (tds.size > 2) tds[2].text().trim() else ""

        val price = if (mainPrice == studentPrice || studentPrice == "") {
            mainPrice
        } else if (mainPrice == "") {
            studentPrice
        } else {
            "$mainPrice / $studentPrice"
        }

        mealsBuilder.addMeal(Meals.Meal(mealName, price))
    }

    companion object {

        private val dateFormatter
            get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
