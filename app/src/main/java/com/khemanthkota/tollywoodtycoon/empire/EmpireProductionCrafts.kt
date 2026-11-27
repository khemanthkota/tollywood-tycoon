package com.khemanthkota.tollywoodtycoon.empire

enum class CraftStage { PRE_PRODUCTION, SHOOTING, POST_PRODUCTION, RELEASE }
enum class FilmCraft(val stage: CraftStage, val weight: Int) {
    WRITING(CraftStage.PRE_PRODUCTION, 10),
    DIRECTION(CraftStage.PRE_PRODUCTION, 10),
    CASTING(CraftStage.PRE_PRODUCTION, 6),
    PRODUCTION_MANAGEMENT(CraftStage.PRE_PRODUCTION, 6),
    ART_DIRECTION(CraftStage.PRE_PRODUCTION, 5),
    COSTUME(CraftStage.PRE_PRODUCTION, 3),
    LOCATION(CraftStage.PRE_PRODUCTION, 3),
    CINEMATOGRAPHY(CraftStage.SHOOTING, 8),
    LIGHTING(CraftStage.SHOOTING, 4),
    ACTING(CraftStage.SHOOTING, 8),
    MAKEUP(CraftStage.SHOOTING, 3),
    STUNTS(CraftStage.SHOOTING, 3),
    CHOREOGRAPHY(CraftStage.SHOOTING, 3),
    PRODUCTION_SOUND(CraftStage.SHOOTING, 4),
    SET_CONSTRUCTION(CraftStage.SHOOTING, 4),
    PROPS(CraftStage.SHOOTING, 2),
    EDITING(CraftStage.POST_PRODUCTION, 7),
    VISUAL_EFFECTS(CraftStage.POST_PRODUCTION, 5),
    MUSIC(CraftStage.POST_PRODUCTION, 5),
    SOUND_DESIGN(CraftStage.POST_PRODUCTION, 4),
    DUBBING(CraftStage.POST_PRODUCTION, 2),
    COLOR_GRADING(CraftStage.POST_PRODUCTION, 3),
    PUBLICITY(CraftStage.RELEASE, 4),
    DISTRIBUTION(CraftStage.RELEASE, 4)
}
enum class CraftInvestment(val cost: Int, val quality: Int) {
    BASIC(0,50), STANDARD(5,70), PREMIUM(15,90)
}
data class CraftAssessment(val extraCost: Int, val quality: Int)
object EmpireProductionCrafts {
    fun available(completed: Int, stage: CraftStage): List<FilmCraft> =
        FilmCraft.values().filter { it.stage == stage }.let { crafts ->
            when { completed < 2 -> emptyList(); completed < 5 -> crafts.take(2)
                completed < 10 -> crafts.take(4); else -> crafts }
        }
    fun assess(choices: Map<FilmCraft,CraftInvestment>): CraftAssessment {
        val totalWeight = FilmCraft.values().sumOf { it.weight }
        val score = FilmCraft.values().sumOf { it.weight * (choices[it] ?: CraftInvestment.STANDARD).quality }
        return CraftAssessment(choices.values.sumOf { it.cost },score/totalWeight)
    }
}
