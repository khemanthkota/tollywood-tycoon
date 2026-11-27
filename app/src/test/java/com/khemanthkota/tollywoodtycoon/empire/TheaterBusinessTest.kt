package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class TheaterBusinessTest {
    private val theater = OwnedTheater("t1","Hyderabad",1,100,500.0,0.1)
    private val booking = Screening("f1",false,10,4,1.0,250.0,100.0,.5,.5)
    @Test fun competitorMovieGeneratesProfit() {
        val result = TheaterBusiness.screen(theater,booking)
        assertEquals(10.0,result.ticketGross,0.0001)
        assertEquals(6.0,result.theaterProfit,0.0001)
    }
    @Test fun ownFilmEliminatesInternalTransfer() {
        val result = TheaterBusiness.screen(theater,booking.copy(ownFilm=true))
        assertEquals(5.0,result.internalTransfer,0.0001)
        assertEquals(11.0,result.consolidatedContribution,0.0001)
    }
    @Test fun emptyTheaterStillCostsMoney() {
        assertEquals(-1.0,TheaterBusiness.screen(theater,booking.copy(occupancy=0.0)).theaterProfit,0.0001)
    }
}
