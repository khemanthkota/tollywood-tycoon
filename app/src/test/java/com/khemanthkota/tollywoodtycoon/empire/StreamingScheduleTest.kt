package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test

class StreamingScheduleTest {
    @Test fun contractualWindowUsesActualReleaseDay() {
        val offer = RightsOffer("StreamVista",RightsCategory.OTT,150,50,300,56)
        val payment = StreamingPayment.from("Film",offer,100,300)
        assertEquals(156,payment.dueDay)
        assertEquals(200,payment.amount)
        assertEquals(150,StreamingPayment.from("Film",offer,100,299).amount)
        assertEquals(0,settleStreaming(listOf(payment),155).received)
        val settled = settleStreaming(listOf(payment),156)
        assertEquals(200,settled.received)
        assertTrue(settled.remaining.isEmpty())
        assertEquals(0,settleStreaming(settled.remaining,157).received)
    }
    @Test fun multipleMoviesAndOverduePayments() {
        val direct = RightsOffer("PremierePlay",RightsCategory.OTT,220,directStreaming=true)
        val a = StreamingPayment.from("A",direct,10,0)
        val b = StreamingPayment("B","CinemaPlus",40,100)
        assertEquals(10,a.dueDay)
        val settled = settleStreaming(listOf(a,b),20)
        assertEquals(220,settled.received)
        assertEquals(listOf(b),settled.remaining)
        assertEquals(100,settleStreaming(settled.remaining,41).received)
    }
}
