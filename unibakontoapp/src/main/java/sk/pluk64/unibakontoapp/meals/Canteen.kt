package sk.pluk64.unibakontoapp.meals

import sk.pluk64.unibakonto.Util

enum class Canteen {
    EAM, VENZA;

    val menu: Meals
        @Throws(Util.ConnectionFailedException::class)
        get() = UnibaDinnerServiceMealsProvider(this).menu

    val fbId: String
        get() = when (this) {
            EAM -> "164741110224671"
            VENZA -> "1744845135734777"
        }

    val fbUri: String
        get() = when (this) {
            EAM -> "https://www.facebook.com/pg/eatandmeetmlyny/posts/"
            VENZA -> "https://www.facebook.com/pg/internatmlyny/posts/"
        }

    val igUri: String
        get() = when (this) {
            EAM -> "https://www.instagram.com/eatandmeetmlyny/"
            VENZA -> "https://www.instagram.com/internatmlyny/"
        }

    val websiteUri: String
        get() = when (this) {
            EAM -> "https://mlyny.uniba.sk/stravovanie/eat-and-meet/denne-menu-eat-and-meet/"
            VENZA -> "https://mlyny.uniba.sk/stravovanie/venza/denne-menu-venza/"
        }
}
