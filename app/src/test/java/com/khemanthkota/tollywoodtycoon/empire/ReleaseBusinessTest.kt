package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class ReleaseBusinessTest {
    private val offer = RightsOffer("Stream A",RightsCategory.OTT,200,50,500)
    @Test fun guaranteedAndConditionalRevenue() {
        val book = RightsBook().accept(offer)
        assertEquals(200L,book.guaranteedRevenue)
        assertEquals(200L,book.totalRevenue(499))
        assertEquals(250L,book.totalRevenue(500))
    }
    @Test(expected=IllegalArgumentException::class) fun cannotSellSameRightsTwice() {
        RightsBook().accept(offer).accept(offer.copy(buyer="Stream B"))
    }
    @Test fun surprisesAreReproducibleAndVaried() {
        assertEquals(ReleaseSurprises.outcome(42,70),ReleaseSurprises.outcome(42,70))
        assertEquals(AudienceSurprise.values().toSet(),(0..999).map { ReleaseSurprises.outcome(it,70) }.toSet())
    }
    @Test fun directStreamingSettlesOnlyContractRevenue() {
        val career = Career(seed=42)
        val production = EmpireEngine.start(career, FilmPlan("Streaming film",Genre.COMEDY,Talent.NEWCOMER,Director.DEBUT,200))
        val deal = RightsOffer("PremierePlay",RightsCategory.OTT,250,directStreaming=true)
        val (next,film) = EmpireEngine.finishWithRights(career,production,null,emptyMap(),RightsBook().accept(deal))
        assertEquals(0,film.gross)
        assertEquals(250,film.rights)
        assertEquals(250,film.revenue)
        assertEquals(50,film.profit)
        assertEquals(1050,next.cash)
    }
    @Test fun theatricalStreamingAddsContractToProducerShare() {
        val career = Career(seed=42)
        val production = EmpireEngine.start(career, FilmPlan("Theatrical film",Genre.COMEDY,Talent.NEWCOMER,Director.DEBUT,200))
        val deal = RightsOffer("StreamVista",RightsCategory.OTT,150,theatricalWindowDays=56)
        val (_,film) = EmpireEngine.finishWithRights(career,production,null,emptyMap(),RightsBook().accept(deal))
        assertTrue(film.gross>0)
        assertEquals(150,film.rights)
        assertEquals(Math.round(film.gross*.45).toInt()+150,film.revenue)
    }
}
