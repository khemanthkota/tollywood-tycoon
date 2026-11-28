package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test

class PostReleaseStreamingTest {
    private val film = FilmResult("Film",75,400,0,180,200,0,100,false)
    @Test fun eligibilityAndExpiry() {
        assertTrue(PostReleaseStreaming.offers(film,100,106,false).isEmpty())
        assertEquals(2,PostReleaseStreaming.offers(film,100,107,false).size)
        assertTrue(PostReleaseStreaming.offers(film,100,107,true).isEmpty())
        assertTrue(PostReleaseStreaming.offers(film.copy(gross=0),100,107,false).isEmpty())
        assertTrue(PostReleaseStreaming.offers(film,100,191,false).isEmpty())
    }
    @Test fun hitsEarnMoreAndOlderFilmsLoseValue() {
        fun price(f: FilmResult,day: Int) = PostReleaseStreaming.offers(f,100,day,false).first().guaranteedLakh
        assertTrue(price(film.copy(gross=800),110)>price(film,110))
        assertTrue(price(film,110)>price(film,180))
    }
    @Test fun paymentRespectsWindowAndAcceptanceDate() {
        val offer = PostReleaseStreaming.offers(film,100,110,false).first()
        assertEquals(156,PostReleaseStreaming.payment(film,offer,100,110).dueDay)
        assertEquals(170,PostReleaseStreaming.payment(film,offer,100,170).dueDay)
    }
}
