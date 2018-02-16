package sk.pluk64.unibakonto.meals;

import sk.pluk64.unibakonto.http.Util;

interface MealsProvider {
    Meals getMenu() throws Util.ConnectionFailedException;
}
