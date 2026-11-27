package com.khemanthkota.tollywoodtycoon.empire

internal data class FilmSchedule(
    val startDay: Int,
    val shootingDays: Int,
    val postDays: Int,
    val releaseDay: Int,
    val dailyOverheadLakh: Double,
    val financedLakh: Double = 0.0,
    val annualRate: Double = 0.0,
    val missedReleaseCostLakh: Double = 0.0
) {
    init {
        require(startDay >= 0 && shootingDays > 0 && postDays >= 0)
        require(releaseDay.toLong() >= plannedCompletion)
        require(listOf(dailyOverheadLakh, financedLakh, annualRate, missedReleaseCostLakh).all { it.isFinite() && it >= 0 })
    }
    val plannedCompletion: Long get() = startDay.toLong() + shootingDays + postDays
    fun costs(day: Int, completedDay: Int? = null): ScheduleCosts {
        require(day >= startDay)
        require(completedDay == null || completedDay in startDay..day)
        val finish = (completedDay ?: day).toLong()
        val late = (finish - plannedCompletion).coerceAtLeast(0)
        val elapsed = day.toLong() - startDay
        return ScheduleCosts(late, late * dailyOverheadLakh,
            financedLakh * annualRate * elapsed / 365.0,
            if (finish > releaseDay) missedReleaseCostLakh else 0.0)
    }
}
internal data class ScheduleCosts(val delayDays: Long, val overheadLakh: Double, val interestLakh: Double, val releasePenaltyLakh: Double) {
    val totalLakh: Double get() = overheadLakh + interestLakh + releasePenaltyLakh
}
internal data class StudioCalendar(val day: Int = 0) {
    init { require(day >= 0) }
    fun advance(days: Int): StudioCalendar {
        require(days > 0 && days <= Int.MAX_VALUE - day)
        return copy(day = day + days)
    }
}
