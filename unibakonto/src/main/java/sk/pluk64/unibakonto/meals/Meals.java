package sk.pluk64.unibakonto.meals;

import java.util.ArrayList;
import java.util.List;

public class Meals {
    public final List<DayMenu> menus;

    public Meals(List<DayMenu> menus) {
        this.menus = menus;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Meals{" +
                "menus=" + menus +
                '}';
    }

    static class Builder {
        private final List<DayMenu> menus = new ArrayList<>();
        private DayMenu curDayMenu;
        private SubMenu curSubMenu;

        public void newDay(String day) {
            curDayMenu = new DayMenu(day);
            menus.add(curDayMenu);
        }

        public void newSubMenu(String text) {
            curSubMenu = new SubMenu(text);
            curDayMenu.add(curSubMenu);
        }

        public void addMeal(Meal meal) {
            curDayMenu.curSubMenu.meals.add(meal);
        }

        public Meals build() {
            return new Meals(menus);
        }
    }

    public static class DayMenu {
        public final String dayName;
        public final List<SubMenu> subMenus = new ArrayList<>();
        public SubMenu curSubMenu;

        public DayMenu(String dayName) {
            this.dayName = dayName;
        }

        public void add(SubMenu subMenu) {
            subMenus.add(subMenu);
            curSubMenu = subMenu;
        }
    }

    public static class SubMenu {
        public final String name;
        public final List<Meal> meals = new ArrayList<>();

        public SubMenu(String name) {
            this.name = name;
        }
    }

    public static class Meal {
        public final String name;
        public final String price;

        public Meal(String name, String price) {
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Meal{" +
                    "name='" + name + '\'' +
                    ", price='" + price + '\'' +
                    '}';
        }
    }
}
