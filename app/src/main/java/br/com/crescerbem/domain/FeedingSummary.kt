package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import java.time.LocalDate

data class FeedingDaySummary(
    val total: Int = 0,
    val breastfeedings: Int = 0,
    val breastMinutes: Int = 0,
    val formulaCount: Int = 0,
    val formulaMl: Int = 0,
    val waterMl: Int = 0,
    val solidMeals: Int = 0,
    val newFoods: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = total == 0
}

object FeedingSummaries {
    fun summarize(feedings: List<Feeding>): FeedingDaySummary {
        val breast = feedings.filter { it.type == FeedingType.BREAST_MILK }
        val formula = feedings.filter { it.type == FeedingType.FORMULA }
        return FeedingDaySummary(
            total = feedings.size,
            breastfeedings = breast.size,
            breastMinutes = breast.sumOf { it.durationMinutes ?: 0 },
            formulaCount = formula.size,
            formulaMl = formula.sumOf { it.amountMl ?: 0 },
            waterMl = feedings.filter { it.type == FeedingType.WATER }.sumOf { it.amountMl ?: 0 },
            solidMeals = feedings.count { it.type.isSolid },
            newFoods = feedings
                .filter { it.firstTime && !it.food.isNullOrBlank() }
                .map { it.food!!.trim() }
                .distinctBy { it.lowercase() },
        )
    }

    /** Resumo por dia, em ordem cronológica. */
    fun byDay(feedings: List<Feeding>): List<Pair<LocalDate, FeedingDaySummary>> =
        feedings.groupBy { it.dateTime.toLocalDate() }
            .toSortedMap()
            .map { (day, items) -> day to summarize(items) }
}
