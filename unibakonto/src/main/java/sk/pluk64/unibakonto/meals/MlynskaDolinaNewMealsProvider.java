package sk.pluk64.unibakonto.meals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLConnection;

import sk.pluk64.unibakonto.Utils;
import sk.pluk64.unibakonto.http.UnibaKonto;
import sk.pluk64.unibakonto.http.Util;

public class MlynskaDolinaNewMealsProvider implements MealsProvider {
    private final String location;

    public MlynskaDolinaNewMealsProvider(Menza menza) {
        switch (menza) {
            case EAM:
                location = "http://mlynska-dolina.sk/stravovanie/eat-meet-denne-menu/";
                break;
            case VENZA:
                location = "http://mlynska-dolina.sk/stravovanie/venza-denne-menu/";
                break;
            default:
                location = "";
        }
    }

    @Override
    public Meals getMenu() throws Util.ConnectionFailedException {
        try {
            URLConnection urlConnection = UnibaKonto.httpGet(location);
            Document doc = Jsoup.parse(Util.connInput2String(urlConnection));
            Meals.Builder mealsBuilder = Meals.builder();

            Elements days = doc.select(".et_pb_toggle");
            for (Element day : days) {
                parseDay(mealsBuilder, day);
            }

            return mealsBuilder.build();
        } catch (IOException e) {
            throw new Util.ConnectionFailedException();
        }
    }

    private void parseDay(Meals.Builder mealsBuilder, Element day) {
        String dayTitle = Utils.getFirstOrEmpty(day.select(".et_pb_toggle_title"));
        mealsBuilder.newDay(dayTitle);

        Elements subMenus = day.select(".mdm_menu_cat_wrapper");
        for (Element subMenu : subMenus) {
            parseSubMenu(mealsBuilder, subMenu);
        }
    }

    private void parseSubMenu(Meals.Builder mealsBuilder, Element subMenu) {
        String subMenuTitle = Utils.getFirstOrEmpty(subMenu.select("p > strong"));
        mealsBuilder.newSubMenu(subMenuTitle);

        Elements meals = subMenu.select(".mdm_menu_item");
        for (Element meal : meals) {
            parseMeal(mealsBuilder, meal);
        }
    }

    private void parseMeal(Meals.Builder mealsBuilder, Element meal) {
        String mealName = Utils.getFirstOrEmpty(meal.select(".mdm_menu_item_left"));
        String mealPrice = Utils.getFirstOrEmpty(meal.select(".mdm_menu_item_right"));

        String parsedMealPrice =
                mealPrice.substring(0, mealPrice.length() - 1)
                        .replace("\u00a0", "");

        String[] split = parsedMealPrice.split("/");
        if (split.length == 2 && split[0].equals(split[1])) {
            parsedMealPrice = split[0];
        }

        mealsBuilder.addMeal(new Meals.Meal(mealName, parsedMealPrice));
    }
}