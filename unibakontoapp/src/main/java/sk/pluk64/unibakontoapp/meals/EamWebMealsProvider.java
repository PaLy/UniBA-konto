package sk.pluk64.unibakontoapp.meals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Calendar;

import sk.pluk64.unibakonto.UnibaKonto;
import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.Utils;

public class EamWebMealsProvider implements MealsProvider {
    @Override
    public Meals getMenu() throws Util.ConnectionFailedException {
        try {
            return getMealsFromEamWeb(getEamMenuUrl(Calendar.getInstance()));
        } catch (Util.ConnectionFailedException ignored) {
        }

        return getMealsFromEamWeb(getEamUrl());
    }

    private static Meals getMealsFromEamWeb(String eamUrl) throws Util.ConnectionFailedException {
        try {
            URLConnection urlConnection = UnibaKonto.httpGet(eamUrl);
            Document doc = Jsoup.parse(Util.connInput2String(urlConnection));

            Meals.Builder mealsBuilder = Meals.builder();

            String date = Utils.getFirstOrEmpty(doc.select(".active.menu-weekday"));
            mealsBuilder.newDay(date);

            Elements subMenus = doc.select(".field");
            for (Element subMenu : subMenus) {
                String subMenuTitle = Utils.getFirstOrEmpty(subMenu.select(".field-label"));
                mealsBuilder.newSubMenu(subMenuTitle);

                Elements items = subMenu.select(".field-item");
                for (Element item : items) {
                    String mealName = Utils.getFirstOrEmpty(item.select(".dish-name"));
                    if ("LIVE JEDLÁ:".equals(mealName) || "PRÍLOHY:".equals(mealName)) {
                        mealsBuilder.newSubMenu(mealName);
                    } else {
                        String mealPrice = Utils.getFirstOrEmpty(item.select(".dish-price"));
                        mealsBuilder.addMeal(new Meals.Meal(mealName, mealPrice));
                    }
                }
            }

            return mealsBuilder.build();
        } catch (IOException e) {
            throw new Util.ConnectionFailedException();
        }
    }

    private static String getEamUrl() {
        return "http://www.eatandmeet.sk/";
    }

    private static String getEamMenuUrl(Calendar date) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        return String.format("%smenu/%d/%02d/%02d", getEamUrl(), year, month, day);
    }
}
