package com.khemanthkota.tollywoodtycoon.empire

import org.junit.Test
import org.junit.Assert.*

class EmpireLifetimeAndPosterTest {
    @Test fun lateRightsCanTurnFilmIntoHit() {
        val film = FilmResult("Action", 76, 650, 0, 650, 600, 0, genre = Genre.ACTION)
        assertFalse(EmpireMastery.isHit(film))
        val sold = film.copy(rights = 100, revenue = 750, ottSold = true)
        assertTrue(EmpireMastery.isHit(sold))
        assertEquals(Genre.ACTION, sold.genre)
        assertEquals(150, sold.profit)
        assertFalse(EmpireMastery.isHit(sold.copy(quality = 59)))
    }
    @Test fun finalCostCanChangeHitVerdict() {
        val film = FilmResult("Action", 60, 710, 0, 710, 600, 0)
        assertFalse(EmpireMastery.isHit(film))
        assertTrue(EmpireMastery.isHit(film.copy(cost = 590)))
    }
    @Test fun campaignsAvoidPreviousDesignsAndRemainDeterministic() {
        val first = EmpirePosterDesign.campaign(Genre.ACTION, 42)
        assertEquals(PosterFocus.values().toSet(), first.map { it.focus }.toSet())
        assertEquals(first, EmpirePosterDesign.campaign(Genre.ACTION, 42))
        val used = first.map { it.signature }.toSet()
        val second = EmpirePosterDesign.campaign(Genre.ACTION, 42, used)
        assertTrue(second.none { it.signature in used })
        assertEquals(3, second.map { it.signature }.toSet().size)
    }
}
