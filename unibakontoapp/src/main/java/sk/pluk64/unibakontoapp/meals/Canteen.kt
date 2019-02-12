package sk.pluk64.unibakontoapp.meals

import sk.pluk64.unibakonto.Util

enum class Canteen {
    EAM, VENZA;

    val menu: Meals?
        @Throws(Util.ConnectionFailedException::class)
        get() {
            return when (this) {
                EAM -> {
                    var meals: Meals? = null
                    try {
                        meals = MlynskaDolinaNewMealsProvider(EAM).menu
                    } catch (ignored: Util.ConnectionFailedException) {
                    }

                    return if (meals == null || meals.menus.isEmpty()) {
                        EamWebMealsProvider().menu
                    } else {
                        meals
                    }
                }
                VENZA -> MlynskaDolinaNewMealsProvider(VENZA).menu
            }
        }

    val fbId: String
        get() {
            return when (this) {
                EAM -> "164741110224671"
                VENZA -> "1744845135734777"
            }
        }
}
