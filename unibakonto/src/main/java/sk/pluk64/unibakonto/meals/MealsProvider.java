package sk.pluk64.unibakonto.meals;

import sk.pluk64.unibakonto.http.Util;

public interface MealsProvider {
    public Meals getMenu() throws Util.ConnectionFailedException;
}
