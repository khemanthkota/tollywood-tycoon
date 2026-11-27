package com.khemanthkota.tollywoodtycoon.empire

import org.junit.Assert.*
import org.junit.Test

class ProductionPortfolioTest {
    @Test fun concurrentFilmsShareCash() {
        val portfolio = ProductionPortfolio(500, 50).commit(ProductionCommitment("a", 200, 20, 30))
        assertEquals(200L, portfolio.available)
        assertFalse(portfolio.canCommit(ProductionCommitment("b", 201)))
        assertEquals(0L, portfolio.commit(ProductionCommitment("b", 200)).available)
    }

    @Test fun updatingFilmReplacesItsReservation() {
        val portfolio = ProductionPortfolio(500, 50).commit(ProductionCommitment("a", 200))
        assertEquals(150L, portfolio.commit(ProductionCommitment("a", 300)).available)
    }

    @Test fun cancellationReleasesOnlySelectedFilm() {
        val portfolio = ProductionPortfolio(500, 50)
            .commit(ProductionCommitment("a", 200))
            .commit(ProductionCommitment("b", 100))
        assertEquals(350L, portfolio.remove("a").available)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOvercommitment() {
        ProductionPortfolio(100, 20).commit(ProductionCommitment("a", 81))
    }
}
