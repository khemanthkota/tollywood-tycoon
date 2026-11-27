package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class ProductionCalendarTest {
    private val film = FilmSchedule(0,30,10,50,3.0,500.0,0.15,20.0)
    @Test fun delayAndInterest() {
        val cost = film.costs(55)
        assertEquals(15L,cost.delayDays)
        assertEquals(45.0,cost.overheadLakh,0.001)
        assertEquals(500.0*0.15*55/365,cost.interestLakh,0.001)
        assertEquals(20.0,cost.releasePenaltyLakh,0.001)
    }
    @Test fun differentFilmsDifferentDelays() {
        val day = StudioCalendar().advance(45).day
        assertEquals(5L,film.costs(day).delayDays)
        assertEquals(0L,film.copy(shootingDays=40,releaseDay=60).costs(day).delayDays)
    }
    @Test fun completedFilmStopsOverhead() {
        assertEquals(0L,film.costs(60,40).delayDays)
        assertEquals(0.0,film.costs(60,40).releasePenaltyLakh,0.001)
    }
}
