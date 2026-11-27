package com.khemanthkota.tollywoodtycoon.empire

enum class Mastery(val hits: Int, val trust: Int) {
    UNDISCOVERED(0, 0), DEBUT(1, 3), SPECIALIST(3, 8), MASTER(7, 15), LEGEND(15, 25)
}
object EmpireMastery {
    fun level(hits: Int): Mastery = Mastery.values().last { hits >= it.hits }
    fun next(hits: Int): Mastery? = Mastery.values().firstOrNull { hits < it.hits }
    fun isHit(result: FilmResult): Boolean =
        result.quality >= 60 && result.cost > 0 && result.profit.toLong() * 100 >= result.cost.toLong() * 20
    fun multiplier(hits: Int): Double = 1.0 + level(hits).trust / 100.0
}
