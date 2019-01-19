package sk.pluk64.unibakontoapp

interface Refreshable {
    fun refresh()
    fun canRefresh(): Boolean
}
