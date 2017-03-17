package sk.pluk64.unibakonto.meals;

import sk.pluk64.unibakonto.http.Util;

public enum Menza {
    EAM, VENZA;

    public Meals getMenu() throws Util.ConnectionFailedException {
        switch (this) {
            case EAM:
                try {
                    return new MlynskaDolinaNewMealsProvider(EAM).getMenu();
                } catch (Util.ConnectionFailedException ignored) {
                }

                return new EamWebMealsProvider().getMenu();
            case VENZA:
                return new MlynskaDolinaNewMealsProvider(VENZA).getMenu();
            default:
                return null;
        }
    }

    public String getFBid() {
        switch (this) {
            case EAM:
                return "164741110224671";
            case VENZA:
                return "venza.mlyny";
            default:
                return "";
        }
    }
}
