package com.khemanthkota.tollywoodtycoon.empire

internal enum class TheaterAcquisition(val upfront: Int, val monthly: Int, val buildDays: Int) {
    PURCHASE(250, 8, 0), LEASE(40, 18, 0), BUILD(180, 8, 120)
}
internal data class StudioTheater(val id: String, val acquisition: TheaterAcquisition, val openingDay: Int) {
    fun operating(day: Int) = day >= openingDay
    // Exhibitor share only; producer receipts are accounted for separately.
    fun dailyMargin(day: Int, occupancy: Int, ownFilm: Boolean): Int {
        require(occupancy in 0..100)
        if (!operating(day)) return 0
        val gross = occupancy * 12 / 100
        return gross * 55 / 100 + occupancy * 2 / 100 - 2
    }
}
internal data class StudioRelationships(val scores: Map<String, Int> = emptyMap()) {
    fun score(person: String) = scores[person] ?: 50
    fun change(person: String, delta: Int): StudioRelationships {
        require(person.isNotBlank())
        return copy(scores = scores + (person to (score(person).toLong()+delta).coerceIn(0,100).toInt()))
    }
    fun gift(person: String, cost: Int): StudioRelationships {
        require(cost in 1..100)
        return change(person, (cost / 5 + 1).coerceAtMost(15))
    }
    fun platformPremium(buyer: String): Int = (score(buyer)-50)/5
}
