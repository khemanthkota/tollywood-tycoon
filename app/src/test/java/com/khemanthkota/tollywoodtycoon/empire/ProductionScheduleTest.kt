package com.khemanthkota.tollywoodtycoon.empire

import org.junit.Assert.*
import org.junit.Test

class ProductionScheduleTest {
    private fun movie(id: String, day: Int? = null) = ProductionSlot(
        id, Production(FilmPlan(id, Genre.ACTION, Talent.NEWCOMER, Director.DEBUT, 150), false, 42),
        stage = 3, releaseDay = day
    )

    @Test fun switchingAndReschedulingDoesNotOverwriteOtherMovies() {
        val initial = ProductionSlots(listOf(movie("A", 14), movie("B", 150), movie("C", 300)), "A")
        val selected = initial.select("C")
        val changed = selected.save(selected.selected!!.copy(releaseDay = 310), 1000, 0)
        assertEquals(14, changed.select("A").selected!!.releaseDay)
        assertEquals(150, changed.select("B").selected!!.releaseDay)
        assertEquals(310, changed.select("C").selected!!.releaseDay)
        assertEquals(300, initial.select("C").selected!!.releaseDay)
    }

    @Test fun completingOneMovieRetainsOtherDates() {
        val remaining = ProductionSlots(listOf(movie("A", 14), movie("B", 150)), "A").remove("A")
        assertEquals(150, remaining.selected!!.releaseDay)
        assertNull(movie("New").releaseDay)
    }

    @Test fun noAvailableScreensMeansNoTheatricalGross() {
        val career = Career()
        val production = movie("A", 14).production
        val forecast = ReleaseMarketCalendar(releases = listOf(AnnouncedRelease("r", "Rival", 14, "Telugu", 90, 100)))
            .forecast(14, "Telugu", production.plan.cast.appeal, 100)
        val result = EmpireEngine.finishWithRights(career, production, null, emptyMap(), null, forecast).second
        assertEquals(0, result.gross)
    }
}
