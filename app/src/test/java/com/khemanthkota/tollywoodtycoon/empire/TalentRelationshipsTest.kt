package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class TalentRelationshipsTest {
    private val extension = CrewBooking("actor1","film1",10,15)
    @Test fun conflictingFilmCannotBeBoughtOutAutomatically() {
        val calendar = CrewCalendar(listOf(CrewBooking("actor1","film2",12,20)))
        assertEquals(ExtensionDecision.DATE_CONFLICT,calendar.quote(extension,10,1000,TalentBond()).decision)
    }
    @Test fun lowOfferRequiresNegotiation() {
        assertEquals(ExtensionDecision.COUNTEROFFER,CrewCalendar().quote(extension,10,0,TalentBond()).decision)
    }
    @Test fun giftsDoNotPurchaseTrust() {
        val bond = TalentBond()
        assertEquals(bond.trust,bond.gift(true).trust)
        assertTrue(bond.projectOutcome(false,false).trust < bond.trust)
    }
    @Test(expected=IllegalArgumentException::class) fun cannotCommitUnaffordableOffer() {
        CrewCalendar().confirm(extension,10,50,TalentBond(),49)
    }
}
