package com.khemanthkota.tollywoodtycoon.empire

import kotlin.random.Random
import kotlin.math.roundToInt

data class RivalStudio(val name: String, val cash: Int, val preferredGenre: Genre, val ambition: Int)
data class RivalRelease(val studio: String, val genre: Genre, val budget: Int, val gross: Int, val profit: Int)
data class MarketRound(val demand: Map<Genre, Double>, val rivals: List<RivalRelease>) {
    fun multiplier(genre: Genre): Double =
        (demand.getValue(genre) / (1.0 + rivals.count { it.genre == genre } * 0.12)).coerceIn(0.5, 1.5)
}
object EmpireMarket {
    val initialRivals = listOf(
        RivalStudio("Titan Pictures", 1800, Genre.ACTION, 35),
        RivalStudio("Lotus Films", 850, Genre.DRAMA, 18),
        RivalStudio("Laugh Track", 700, Genre.COMEDY, 20),
        RivalStudio("Silver Frame", 1100, Genre.THRILLER, 25),
        RivalStudio("Moonlight Studios", 900, Genre.ROMANCE, 22)
    )
    fun round(seed: Int, studios: List<RivalStudio> = initialRivals): MarketRound {
        val rng = Random(seed xor 0x52AC)
        val demand = Genre.values().associateWith { rng.nextInt(75, 136) / 100.0 }
        val releases = studios.mapNotNull { s ->
            if (s.cash < 115 || rng.nextInt(100) >= 65) return@mapNotNull null
            val budget = (s.cash * s.ambition / 100).coerceAtLeast(115).coerceAtMost(s.cash)
            val genre = if (rng.nextInt(100) < 75) s.preferredGenre else Genre.values().random(rng)
            val gross = (budget * rng.nextInt(80, 321) / 100.0 * demand.getValue(genre)).roundToInt()
            val revenue = (gross * .45 + budget * .2).roundToInt()
            RivalRelease(s.name, genre, budget, gross, revenue - budget)
        }
        return MarketRound(demand, releases)
    }
    fun settle(studios: List<RivalStudio>, round: MarketRound): List<RivalStudio> =
        studios.map { s -> s.copy(cash = s.cash + (round.rivals.firstOrNull { it.studio == s.name }?.profit ?: 0)) }
}
