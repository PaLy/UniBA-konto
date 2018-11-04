package sk.pluk64.unibakontoplayground;

import sk.pluk64.unibakonto.UnibaKonto;

public class TransactionItemFilters {
    static boolean isChickenMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("kura") || desc.contains("kur.");
    }

    static boolean isBeefMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("hovä") || desc.contains("hov.");
    }

    static boolean isPorkMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("brav");
    }

    static boolean isTurkeyMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("morč");
    }

    static boolean isSoup(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return (desc.contains("pol.") || desc.contains("poli") || desc.contains("0,33") || desc.contains("033")
            || desc.contains("boršč")) && !desc.contains("miska");
    }

    static boolean isRiceMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("ryž") && !desc.contains("slov");
    }

    static boolean isPotatoMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("zem") || desc.contains("hranolky");
    }

    static boolean isCheeseMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("syr") || desc.contains("enc");
    }

    static boolean isSaladMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("šalát") && !desc.contains("miska") || desc.contains("cvikla");
    }

    static boolean isDrink(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("dcl") && !desc.contains("plast");
    }

    static boolean isKnodel(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("kned");
    }

    static boolean isEggBarley(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("tarh") || (desc.contains("slov") && desc.contains("ryž"));
    }

    static boolean isFishMeal(UnibaKonto.TransactionItem ti) {
        String desc = ti.description != null ? ti.description.toLowerCase() : "";
        return desc.contains("ryb") || desc.contains("losos") || desc.contains("panga");
    }

    static boolean isOtherMeal(UnibaKonto.TransactionItem ti) {
        return !isBeefMeal(ti) && !isChickenMeal(ti) && !isPorkMeal(ti) && !isPotatoMeal(ti)
            && !isRiceMeal(ti) && !isSoup(ti) && !isTurkeyMeal(ti) && !isCheeseMeal(ti)
            && !isSaladMeal(ti) && !isDrink(ti) && !isKnodel(ti) && !isEggBarley(ti)
            && !isFishMeal(ti);
    }
}
