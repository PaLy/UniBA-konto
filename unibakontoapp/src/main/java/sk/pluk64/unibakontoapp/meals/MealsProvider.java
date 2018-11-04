package sk.pluk64.unibakontoapp.meals;

import sk.pluk64.unibakonto.Util;

interface MealsProvider {
    Meals getMenu() throws Util.ConnectionFailedException;
}
