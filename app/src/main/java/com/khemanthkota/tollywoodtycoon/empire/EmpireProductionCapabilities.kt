package com.khemanthkota.tollywoodtycoon.empire

enum class StoryGenre(val needs: Set<String>) {
    ROMANCE(setOf("camera","sound")), COMEDY(setOf("camera","sound")),
    DRAMA(setOf("camera","sound")), FAMILY(setOf("camera","sound")),
    CRIME(setOf("camera","sets")), THRILLER(setOf("camera","lighting")),
    MYSTERY(setOf("lighting","post")), ACTION(setOf("stunt","camera")),
    HORROR(setOf("lighting","costume")), SCI_FI(setOf("vfx","virtual")),
    FANTASY(setOf("vfx","costume")), HISTORICAL(setOf("sets","costume")),
    MYTHOLOGY(setOf("sets","costume","vfx")), ADVENTURE(setOf("transport","stunt")),
    SPORTS(setOf("camera","stunt")), MUSICAL(setOf("music","sound"))
}
enum class ProductionScale(val target: Int) { INDEPENDENT(1), COMMERCIAL(3), BLOCKBUSTER(6), EPIC(8) }
data class CreativeDirector(val creativity: Int, val technicalSkill: Int,
    val efficiency: Int, val expertise: Set<StoryGenre>) {
    init { require(listOf(creativity,technicalSkill,efficiency).all { it in 0..100 }) }
}
data class HybridFilm(val primary: StoryGenre, val secondary: StoryGenre? = null,
    val tertiary: StoryGenre? = null, val scale: ProductionScale = ProductionScale.INDEPENDENT) {
    val genres get() = listOfNotNull(primary,secondary,tertiary)
    init { require(genres.distinct().size == genres.size && (tertiary == null || secondary != null)) }
}
data class CapabilityAssessment(val available: Map<String,Int>, val gaps: Map<String,Int>,
    val qualityPotential: Int, val innovationBonus: Int, val efficiencyBonus: Int,
    val experimental: Boolean)
object EmpireProductionCapabilities {
    private val variants by lazy { EmpireTechnologyRoadmap.variants.associateBy { it.technology.id } }
    private val legacyCategories = mapOf("digital4k" to "camera","largeformat" to "camera",
        "ledstage" to "virtual","robotcamera" to "camera","volumetric" to "motion",
        "lightfield" to "distribution","haptic" to "distribution","orbital" to "special")
    fun assess(film: HybridFilm, director: CreativeDirector, economy: AssetEconomy,
               studio: String, startDay: Int, endDay: Int, hiredCapabilities: Map<String,Int> = emptyMap()): CapabilityAssessment {
        require(endDay > startDay)
        val available = hiredCapabilities.mapValues { it.value.coerceIn(0,10) }.toMutableMap()
        economy.assets.filter { a -> !a.auction && (
            (a.owner == studio && (a.renter == null || a.bookedUntil <= startDay)) ||
            (a.renter == studio && a.bookedUntil >= endDay))
        }.forEach { a ->
            val v = variants[a.technologyId]
            val category = v?.category ?: legacyCategories[a.technologyId]
            val level = v?.generation?.crewTraining ?: 1
            if (category != null) available[category] = maxOf(available[category] ?: 0,level)
        }
        // A capable director can substitute practical effects for CGI, or virtual sets for physical sets.
        if (director.creativity >= 80 && director.technicalSkill >= 70) {
            val practical = minOf(available["costume"] ?: 0, available["lighting"] ?: 0)
            available["vfx"] = maxOf(available["vfx"] ?: 0,(practical-1).coerceAtLeast(0))
            available["sets"] = maxOf(available["sets"] ?: 0,(available["virtual"] ?: 0)-1)
            available["stunt"] = maxOf(available["stunt"] ?: 0,(available["virtual"] ?: 0)-2)
        }
        val needs = film.genres.flatMap { it.needs }.toSet()
        val gaps = needs.associateWith { (film.scale.target-(available[it] ?: 0)).coerceAtLeast(0) }.filterValues { it > 0 }
        val expertise = film.genres.count { it in director.expertise }
        val innovation = if (film.genres.size > 1) director.creativity * expertise / (film.genres.size * 10) else 0
        val quality = (65 + expertise*4 + innovation - gaps.values.sum()*5).coerceIn(0,100)
        return CapabilityAssessment(available,gaps,quality,innovation,director.efficiency/10,
            film.genres.size > 1 && expertise < film.genres.size)
    }
}
