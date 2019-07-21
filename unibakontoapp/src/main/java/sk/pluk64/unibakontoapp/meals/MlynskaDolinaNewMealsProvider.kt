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

class MlynskaDolinaNewMealsProvider(canteen: Canteen) : MealsProvider {
    private val location: String = when (canteen) {
        Canteen.EAM -> "http://mlynska-dolina.sk/stravovanie/denne-menu-eat-meet/"
        Canteen.VENZA -> "http://mlynska-dolina.sk/stravovanie/denne-menu-venza/"
    }

    override val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() {
            try {
                val urlConnection = UnibaKonto.httpGet(location)
                val doc = Jsoup.parse(Util.connInput2String(urlConnection))
                val mealsBuilder = Meals.builder()

                val days = doc.select(".et_pb_toggle")
                for (day in days) {
                    parseDay(mealsBuilder, day)
                }

                return mealsBuilder.build()
            } catch (e: IOException) {
                throw Util.ConnectionFailedException()
            }

        }

    private fun parseDay(mealsBuilder: Meals.Builder, day: Element) {
        val dayTitle = Utils.getFirstOrEmpty(day.select(".et_pb_toggle_title"))

        if (!isOldDay(dayTitle)) {
            mealsBuilder.newDay(dayTitle)

            val subMenus = day.select(".mdm_menu_cat_wrapper")
            for (subMenu in subMenus) {
                parseSubMenu(mealsBuilder, subMenu)
            }
        }
    }

    private fun isOldDay(dayTitle: String): Boolean {
        val dateStart = dayTitle.indexOf('(')
        val dateEnd = dayTitle.indexOf(')')
        if (dateStart != -1 && dateEnd != -1) {
            val date = dayTitle.substring(dateStart + 1, dateEnd)
            try {
                val today = Calendar.getInstance()

                val d = dateFormatter.parse(date)
                val cDate = Calendar.getInstance()
                cDate.time = d

                return today.get(Calendar.YEAR) > cDate.get(Calendar.YEAR) || today.get(Calendar.YEAR) == cDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) > cDate.get(Calendar.DAY_OF_YEAR)
            } catch (ignored: ParseException) {
            }

        }
        return false
    }

    private fun parseSubMenu(mealsBuilder: Meals.Builder, subMenu: Element) {
        val subMenuTitle = Utils.getFirstOrEmpty(subMenu.select("p > strong"))
        mealsBuilder.newSubMenu(subMenuTitle)

        val meals = subMenu.select(".mdm_menu_item")
        for (meal in meals) {
            parseMeal(mealsBuilder, meal)
        }
    }

    private fun parseMeal(mealsBuilder: Meals.Builder, meal: Element) {
        val mealName = Utils.getFirstOrEmpty(meal.select(".mdm_menu_item_left"))
        val mealPrice = Utils.getFirstOrEmpty(meal.select(".mdm_menu_item_right"))

        var parsedMealPrice = mealPrice.substring(0, mealPrice.length - 1)
            .replace("\u00a0", "")

        val split = parsedMealPrice.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size == 2) {
            val first = split[0].trim { it <= ' ' }
            val second = split[1].trim { it <= ' ' }
            if (first == second) {
                parsedMealPrice = first
            }
        }

        mealsBuilder.addMeal(Meals.Meal(mealName, parsedMealPrice))
    }

    companion object {

        private val dateFormatter
            get() = SimpleDateFormat("d.M.yyyy", Locale.US)
    }
}
