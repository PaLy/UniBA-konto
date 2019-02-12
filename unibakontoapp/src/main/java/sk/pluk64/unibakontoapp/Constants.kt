package sk.pluk64.unibakontoapp

import sk.pluk64.unibakontoapp.meals.Canteen

class PreferencesKeys {
    companion object {
        const val VERSION_CODE = "version_code"

        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val SELECTED_PAGE_TABBED_FRAGMENT = "selected_page"

        const val BALANCES = "balances"
        const val CLIENT_NAME = "client_name"
        const val TRANSACTIONS = "transactions"
        const val ALL_TRANSACTIONS = "all_transactions"
        const val ACCOUNT_REFRESH_TIMESTAMP = "account_refresh_timestamp"
        const val TRANSACTIONS_REFRESH_TIMESTAMP = "transactions_refresh_timestamp"
        const val ALL_TRANSACTIONS_REFRESH_TIMESTAMP = "all_transactions_refresh_timestamp_date"
        const val CARDS = "cards"

        const val LOGGED_IN = "logged_in"

        fun meals(canteen: Canteen): String = "meals$canteen"
        fun mealsRefreshTimestamp(canteen: Canteen): String = "meals_refreshed_timestamp$canteen"
        fun mealsPhotos(canteen: Canteen): String = "meals_photos$canteen"
    }
}
