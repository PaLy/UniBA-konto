package sk.pluk64.unibakontoapp.meals

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
class Meals(val menus: List<DayMenu>) {

    override fun toString(): String {
        return "Meals{" +
            "menus=" + menus +
            '}'.toString()
    }

    class Builder {
        private val menus = ArrayList<DayMenu>()
        private var curDayMenu: DayMenu? = null
        private var curSubMenu: SubMenu? = null

        fun newDay(day: String) {
            menus.add(DayMenu(day))
            curDayMenu = menus.last()
        }

        fun newSubMenu(text: String) {
            val newSubMenu = SubMenu(text)
            curSubMenu = newSubMenu
            curDayMenu?.add(newSubMenu)
        }

        fun addMeal(meal: Meal) {
            curDayMenu?.curSubMenu?.meals?.add(meal)
        }

        fun build(): Meals {
            val filteredMenus = menus.asSequence()
                .onEach {
                    it.subMenus.removeAll(
                        it.subMenus.asSequence()
                            .onEach {
                                it.meals.removeAll(
                                    it.meals.asSequence()
                                        .filter { it.name.isBlank() }
                                )
                            }
                            .filter { it.meals.isEmpty() }
                    )
                }
                .filter { it.subMenus.isNotEmpty() }
                .toList()
            return Meals(filteredMenus)
        }
    }

    @Serializable
    class DayMenu(val dayName: String) {
        val subMenus: MutableList<SubMenu> = ArrayList()
        @Transient
        var curSubMenu: SubMenu? = null

        fun add(subMenu: SubMenu) {
            subMenus.add(subMenu)
            curSubMenu = subMenu
        }
    }

    @Serializable
    class SubMenu(val name: String) {
        val meals: MutableList<Meal> = ArrayList()
    }

    @Serializable
    class Meal(val name: String, val price: String) {

        override fun toString(): String {
            return "Meal{" +
                "name='" + name + '\''.toString() +
                ", price='" + price + '\''.toString() +
                '}'.toString()
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
        val EMPTY = Meals(emptyList())
    }
}
