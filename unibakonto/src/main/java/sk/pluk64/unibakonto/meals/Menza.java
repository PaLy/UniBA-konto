package sk.pluk64.unibakonto.meals;

import sk.pluk64.unibakonto.http.Util;

public enum Menza {
    EAM, VENZA;

    public Meals getMenu() throws Util.ConnectionFailedException {
        Meals meals = null;
        switch (this) {
            case EAM:
                try {
                    meals = new MlynskaDolinaNewMealsProvider(EAM).getMenu();
                } catch (Util.ConnectionFailedException ignored) {
                }

                if (meals == null || meals.menus == null || meals.menus.isEmpty()) {
                    meals = new EamWebMealsProvider().getMenu();
                }
                break;
            case VENZA:
                meals = new MlynskaDolinaNewMealsProvider(VENZA).getMenu();
                break;
            default:
                return null;
        }

        if (meals != null && meals.menus != null) {
            return meals;
        } else {
            throw new Util.ConnectionFailedException();
        }
    }

    public String getFBid() {
        switch (this) {
            case EAM:
                return "164741110224671";
            case VENZA:
                return "1744845135734777";
            default:
                return "";
        }
    }
}
