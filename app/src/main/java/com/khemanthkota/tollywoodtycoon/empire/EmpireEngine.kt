package com.khemanthkota.tollywoodtycoon.empire

import kotlin.random.Random
import kotlin.math.roundToInt

// All money is in lakh rupees. Fictional talent and simulated outcomes.
enum class Genre { ACTION, COMEDY, ROMANCE, THRILLER, DRAMA }
enum class Talent(val fee: Int, val skill: Int, val appeal: Int) {
    NEWCOMER(35, 72, 30), ESTABLISHED(120, 78, 65), STAR(280, 82, 90)
}
enum class Director(val fee: Int, val skill: Int) {
    DEBUT(20, 68), EXPERIENCED(75, 82), PREMIUM(150, 91)
}
data class FilmPlan(val title: String, val genre: Genre, val cast: Talent,
    val director: Director, val budget: Int, val hybrid: HybridFilm? = null) {
    val minimumCost get() = cast.fee + director.fee + 60
}
data class Career(val cash: Int = 1000, val completed: Int = 0,
    val credibility: Int = 50, val audience: Int = 20, val seed: Int = 1, val rivals: List<RivalStudio> = EmpireMarket.initialRivals, val genreHits: Map<Genre, Int> = emptyMap()) {
    val unlocked get() = when {
        completed < 2 -> 1
        completed < 5 -> 2
        completed < 10 -> 3
        completed < 15 -> 4
        else -> 5
    }
}
enum class Response(val cost: Int, val quality: Int, val delay: Int) {
    REPAIR(15, 0, 3), RENT(30, 2, 1), COMPROMISE(0, -9, 0)
}
data class Production(val plan: FilmPlan, val event: Boolean, val seed: Int, val capability: CapabilityAssessment? = null)
data class FilmResult(val title: String, val quality: Int, val gross: Int,
    val rights: Int, val revenue: Int, val cost: Int, val delay: Int) {
    val profit get() = revenue - cost
}
object EmpireEngine {
    fun start(career: Career, plan: FilmPlan, economy: AssetEconomy? = null,
              studio: String = "player", startDay: Int = 0, endDay: Int = 30,
              hiredCapabilities: Map<String,Int> = emptyMap()): Production {
        require(plan.title.isNotBlank())
        require(plan.budget in plan.minimumCost..career.cash)
        val random = Random(career.seed)
        val capability = plan.hybrid?.let { film ->
            require(economy != null) { "Hybrid production requires an asset economy" }
            val director = CreativeDirector(plan.director.skill, plan.director.skill,
                plan.director.skill, setOf(film.primary))
            EmpireProductionCapabilities.assess(film,director,economy,studio,startDay,endDay,hiredCapabilities)
        }
        return Production(plan, career.completed >= 2 && random.nextInt(100) < 55, random.nextInt(), capability)
    }
    fun finish(career: Career, production: Production, response: Response? = null, crafts: Map<FilmCraft,CraftInvestment> = emptyMap()): Pair<Career, FilmResult> = finishWithRelease(career,production,response,crafts,null)
    internal fun finishWithRelease(career: Career, production: Production, response: Response?, crafts: Map<FilmCraft,CraftInvestment>, forecast: ReleaseForecast?): Pair<Career, FilmResult> {
        val p = production.plan
        require(p.budget in p.minimumCost..career.cash)
        require(!production.event || response != null)
        val choice = if (production.event) response else null
        val assessment = EmpireProductionCrafts.assess(crafts)
        val cost = p.budget + (choice?.cost ?: 0) + assessment.extraCost
        require(cost <= career.cash)
        val rng = Random(production.seed)
        val craft = ((p.budget - p.cast.fee - p.director.fee) / 12).coerceIn(0, 20)
        val baseQuality = (p.cast.skill * .25 + p.director.skill * .45 + craft + rng.nextInt(-12, 13) + (choice?.quality ?: 0)).roundToInt().coerceIn(0, 100)
        val adjustedQuality = (baseQuality + (assessment.quality-70)*.5).roundToInt().coerceIn(0,100)
        val quality = production.capability?.let {
            (adjustedQuality * .65 + it.qualityPotential * .35).roundToInt().coerceIn(0,100)
        } ?: adjustedQuality
        val genreDemand = when (p.genre) {
            Genre.ACTION -> 1.15
            Genre.COMEDY -> 1.05
            Genre.ROMANCE -> 1.0
            Genre.THRILLER -> 0.95
            Genre.DRAMA -> 0.85
        }
        val market = EmpireMarket.round(career.seed, career.rivals)
        val demand = rng.nextInt(65, 136) / 100.0 * genreDemand * market.multiplier(p.genre) * EmpireMastery.multiplier(career.genreHits[p.genre] ?: 0)
        val opening = (30 + p.cast.appeal * 2 + career.audience) * demand
        val releaseFactor = forecast?.let { it.occupancy / (.15 + p.cast.appeal * .006) } ?: 1.0
        val gross = (opening * (0.8 + 5 * (quality / 100.0) * (quality / 100.0)) * releaseFactor).roundToInt()
        val rights = (p.cast.appeal * 1.2 + quality * .8).roundToInt()
        val revenue = (gross * .45).roundToInt() + rights
        val result = FilmResult(p.title, quality, gross, rights, revenue, cost, choice?.delay ?: 0)
        val next = career.copy(cash = career.cash + result.profit, completed = career.completed + 1,
            credibility = (career.credibility + if (result.profit >= 0) 4 else -5).coerceIn(0, 100),
            audience = (career.audience + (quality - 60) / 5).coerceIn(0, 100), seed = rng.nextInt(), rivals = EmpireMarket.settle(career.rivals, market), genreHits = if (EmpireMastery.isHit(result)) career.genreHits + (p.genre to ((career.genreHits[p.genre] ?: 0) + 1)) else career.genreHits)
        return next to result
    }
}
