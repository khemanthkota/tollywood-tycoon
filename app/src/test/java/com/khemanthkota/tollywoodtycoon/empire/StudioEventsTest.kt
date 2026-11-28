package com.khemanthkota.tollywoodtycoon.empire

import org.junit.Test
import org.junit.Assert.*

class StudioEventsTest {
    @Test fun targetsStableWithinCycle() {
        EmpireMarket.initialRivals.forEach {
            assertEquals(StudioEvents.targetDay(it,0),StudioEvents.targetDay(it,27))
            assertTrue(StudioEvents.targetDay(it,0) in 28..55)
        }
    }
    @Test fun rivalOffersRespectBudget() {
        Talent.values().forEach { talent ->
            assertEquals(0,StudioEvents.rivalBid(emptyList(),talent,0,1))
            EmpireMarket.initialRivals.forEach { rival ->
                assertTrue(StudioEvents.rivalBid(listOf(rival),talent,0,1)<=rival.cash/3)
            }
        }
    }
    @Test fun managerProtectsReservedFunds() {
        assertFalse(StudioEvents.managerGift(150,100,50))
        assertTrue(StudioEvents.managerGift(200,100,50))
        assertFalse(StudioEvents.managerGift(200,100,70))
    }
    @Test fun distressAndResale() {
        assertTrue(StudioEvents.distressed(10,11))
        assertFalse(StudioEvents.distressed(10,10))
        assertEquals(0,StudioEvents.resale(StudioTheater("lease",TheaterAcquisition.LEASE,0)))
        assertEquals(150,StudioEvents.resale(StudioTheater("owned",TheaterAcquisition.PURCHASE,0)))
    }
}
