package sk.pluk64.unibakonto.meals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLConnection;

import sk.pluk64.unibakonto.http.UnibaKonto;
import sk.pluk64.unibakonto.http.Util;

public class MlynskaDolinaMealsProvider implements MealsProvider {
    private final String location;
    private final int someParam;

    public MlynskaDolinaMealsProvider(Menza menza) {
        switch (menza) {
            case EAM:
                location = "http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/eat-meet/denne-menu";
                someParam = 4;
                break;
            case VENZA:
                location = "http://mlynska-dolina.sk/stravovanie/vsetky-zariadenia/venza/denne-menu";
                someParam = 3;
                break;
            default:
                location = "";
                someParam = -1;
        }
    }

    public Meals getMenu() throws Util.ConnectionFailedException {
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

            return mealsBuilder.build();
        } catch (IOException e) {
            throw new Util.ConnectionFailedException();
        }
    }
}
