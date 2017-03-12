package sk.pluk64.unibakonto.meals;

import sk.pluk64.unibakonto.http.Util;

public enum Menza {
    EAM, VENZA;

    public Meals getMenu() throws Util.ConnectionFailedException {
        switch (this) {
            case EAM:
                try {
                    new MlynskaDolinaMealsProvider(EAM).getMenu();
                } catch (Util.ConnectionFailedException ignored) {
                }

                return new EamWebMealsProvider().getMenu();
            case VENZA:
                return new MlynskaDolinaMealsProvider(VENZA).getMenu();
            default:
                return null;
        }
    }
}
