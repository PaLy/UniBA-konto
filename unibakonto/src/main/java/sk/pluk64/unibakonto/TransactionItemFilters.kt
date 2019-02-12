package sk.pluk64.unibakonto

object TransactionItemFilters {
    fun isChickenMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("kura") || desc.contains("kur.")
    }

    fun isBeefMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("hovä") || desc.contains("hov.")
    }

    fun isPorkMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("brav")
    }

    fun isTurkeyMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("morč")
    }

    fun isSoup(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return (desc.contains("pol.") || desc.contains("poli") || desc.contains("0,33") || desc.contains("033")
            || desc.contains("boršč")) && !desc.contains("miska")
    }

    fun isRiceMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("ryž") && !desc.contains("slov")
    }

    fun isPotatoMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("zem") || desc.contains("hranolky")
    }

    fun isCheeseMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("syr") || desc.contains("enc")
    }

    fun isSaladMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("šalát") && !desc.contains("miska") || desc.contains("cvikla")
    }

    fun isDrink(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("dcl") && !desc.contains("plast")
    }

    fun isKnodel(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("kned")
    }

    fun isEggBarley(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("tarh") || desc.contains("slov") && desc.contains("ryž")
    }

    fun isFishMeal(ti: TransactionItem): Boolean {
        val desc = ti.description.toLowerCase()
        return desc.contains("ryb") || desc.contains("losos") || desc.contains("panga")
    }

    fun isOtherMeal(ti: TransactionItem): Boolean {
        return (!isBeefMeal(ti) && !isChickenMeal(ti) && !isPorkMeal(ti) && !isPotatoMeal(ti)
            && !isRiceMeal(ti) && !isSoup(ti) && !isTurkeyMeal(ti) && !isCheeseMeal(ti)
            && !isSaladMeal(ti) && !isDrink(ti) && !isKnodel(ti) && !isEggBarley(ti)
            && !isFishMeal(ti))
    }
}
