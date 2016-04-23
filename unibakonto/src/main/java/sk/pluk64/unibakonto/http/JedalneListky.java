package sk.pluk64.unibakonto.http;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class JedalneListky {
    public enum Jedalne {
        EAM, VENZA;

        public Meals getMenu() throws Util.ConnectionFailedException {
            switch (this) {
                case EAM:
                    return getEaM();
                case VENZA:
                    return getVenza();
                default:
                    return null;
            }
        }
    }

    private static Meals getEaM() throws Util.ConnectionFailedException {
        return getMeals("http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/eat-meet/denne-menu", 4);
    }

    private static Meals getVenza() throws Util.ConnectionFailedException {
        return getMeals("http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/venza/denne-menu", 3); // den pred otvorenim jedalne bol dostupny listok na dalsie dni
    }

    private static Meals getMeals(String location, int someParam) throws Util.ConnectionFailedException {
        try {
            URLConnection urlConnection = UnibaKonto.httpGet(location);
            Document doc = Jsoup.parse(Util.connInput2String(urlConnection));
            Elements elements = doc.select(".field-content");
//            System.out.println(elements.size());
//            System.out.println(elements.get(4).children());

            Meals.Builder mealsBuilder = Meals.builder();
            for (Element e : elements.get(someParam).children()) {
                if (e.tagName().equals("h2")) {
                    mealsBuilder.newDay(e.select("a").text());
                } else if (e.tagName().equals("p")) {
                    Elements strong = e.select("strong");
                    if (!strong.isEmpty()) {
                        mealsBuilder.newSubMenu(strong.get(0).text());
                    }
                } else if (e.tagName().equals("table")) {
                    Elements meals = e.select("tr");
                    for (Element meal : meals) {
                        Elements cols = meal.select("td");
                        String name = cols.get(0).text().replace("\u00a0", "");
                        String cost = cols.get(1).text().replace("\u00a0", "");
                        int n = cost.length();
                        cost = cost.substring(n - 6, n - 1); // TODO
                        mealsBuilder.addMeal(new Meals.Meal(name, cost));
                    }
                }
            }
            Meals meals = mealsBuilder.build();
            return meals;
        } catch (IOException e) {
            throw new Util.ConnectionFailedException();
        }
    }

    public static class Meals {
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

        private static class Builder {
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

    public static void main(String[] args) {
        try {
            System.out.println(getEaM());
            System.out.println(getVenza());
        } catch (Util.ConnectionFailedException e) {
            e.printStackTrace();
        }
    }
}
