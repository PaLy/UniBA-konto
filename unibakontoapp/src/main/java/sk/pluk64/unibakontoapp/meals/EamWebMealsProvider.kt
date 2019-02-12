package sk.pluk64.unibakontoapp.meals

import org.jsoup.Jsoup
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.Utils
import java.io.IOException
import java.util.*

class EamWebMealsProvider : MealsProvider {

    private val eamUrl = "http://www.eatandmeet.sk/"

    override val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() {
            try {
                return getMealsFromEamWeb(getEamMenuUrl(Calendar.getInstance()))
            } catch (ignored: Util.ConnectionFailedException) {
            }
            return getMealsFromEamWeb(eamUrl)
        }

    @Throws(Util.ConnectionFailedException::class)
    private fun getMealsFromEamWeb(eamUrl: String): Meals {
        try {
            val urlConnection = UnibaKonto.httpGet(eamUrl)
            val doc = Jsoup.parse(Util.connInput2String(urlConnection))

            val mealsBuilder = Meals.builder()

            val date = Utils.getFirstOrEmpty(doc.select(".active.menu-weekday"))
            mealsBuilder.newDay(date)

            val subMenus = doc.select(".field")
            for (subMenu in subMenus) {
                val subMenuTitle = Utils.getFirstOrEmpty(subMenu.select(".field-label"))
                mealsBuilder.newSubMenu(subMenuTitle)

                val items = subMenu.select(".field-item")
                for (item in items) {
                    val mealName = Utils.getFirstOrEmpty(item.select(".dish-name"))
                    if ("LIVE JEDLÁ:" == mealName || "PRÍLOHY:" == mealName) {
                        mealsBuilder.newSubMenu(mealName)
                    } else {
                        val mealPrice = Utils.getFirstOrEmpty(item.select(".dish-price"))
                        mealsBuilder.addMeal(Meals.Meal(mealName, mealPrice))
                    }
                }
            }

            return mealsBuilder.build()
        } catch (e: IOException) {
            throw Util.ConnectionFailedException()
        }

    }

    private fun getEamMenuUrl(date: Calendar): String {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        return String.format("%smenu/%d/%02d/%02d", eamUrl, year, month, day)
    }
}
