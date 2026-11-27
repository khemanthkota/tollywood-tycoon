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
}
