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

    @Test fun occupancyRespondsToMarketConditions() {
        val baseline = TheaterDemand.occupancy(80,1.0,200.0,200.0,0,0)
        assertTrue(TheaterDemand.occupancy(80,1.0,400.0,200.0,0,0) < baseline)
        assertTrue(TheaterDemand.occupancy(80,1.0,200.0,200.0,3,0) < baseline)
        assertTrue(TheaterDemand.occupancy(80,1.0,200.0,200.0,0,20) < baseline)
        assertTrue(TheaterDemand.occupancy(100,2.0,100.0,200.0,0,0) <= 1.0)
    }

    @Test fun managerCanPreferCompetitorHit() {
        val own = booking.copy(filmId="own",ownFilm=true,occupancy=.1)
        val rival = booking.copy(filmId="rival",ownFilm=false,occupancy=.9)
        assertEquals("rival",TheaterManager.recommend(theater,listOf(own,rival)).first().screening.filmId)
    }
    @Test fun multiplexAllowsParallelScreens() {
        val portfolio = TheaterPortfolio(listOf(theater.copy(screens=2)))
        val calendar = ExhibitionCalendar().book(ScreeningContract("a","t1",0,booking,0),portfolio)
            .book(ScreeningContract("b","t1",0,booking,1),portfolio)
        val (settled,profit) = calendar.settle(10,portfolio)
        assertEquals(13.0,profit,0.0001)
        assertEquals(0.0,settled.settle(10,portfolio).second,0.0001)
    }
}
