package com.khemanthkota.tollywoodtycoon.empire

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CareerStoreTest {
    @Test fun activeAndParkedDatesSurviveReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("cinema_empire_v1", 0)
        val original = prefs.getString("career", null)
        try {
            fun film(title: String) = Production(FilmPlan(title, Genre.ACTION, Talent.NEWCOMER, Director.DEBUT, 150), false, 42)
            val slots = ProductionSlots(listOf(
                ProductionSlot("B", film("B"), 3, releaseDay = 150),
                ProductionSlot("C", film("C"), 3, releaseDay = 300)
            ))
            assertTrue(CareerStore(context).write(Save(pending = film("A"), stage = 3, slots = slots, releaseDay = 14)))
            val restored = CareerStore(context).load()
            assertEquals("A", restored.pending?.plan?.title)
            assertEquals(14, restored.releaseDay)
            assertEquals(mapOf("B" to 150, "C" to 300), restored.slots.movies.associate { it.id to it.releaseDay })
        } finally {
            val editor = prefs.edit()
            if (original == null) editor.remove("career") else editor.putString("career", original)
            editor.commit()
        }
    }
    @Test fun ottContractsAndNegotiationsSurviveReloadIndependently() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("cinema_empire_v1", 0)
        val original = prefs.getString("career", null)
        try {
            fun film(title: String) = Production(FilmPlan(title, Genre.ACTION, Talent.NEWCOMER, Director.DEBUT, 150), false, 42)
            val signed = RightsOffer("PremierePlay",RightsCategory.OTT,220,directStreaming=true)
            val open = RightsNegotiation(RightsOffer("StreamVista",RightsCategory.OTT,140,theatricalWindowDays=56),1)
            val rejected = RightsNegotiation(RightsOffer("CinemaPlus",RightsCategory.OTT,100,50,300,28),2,DealStatus.WALKED_AWAY)
            val parked = ProductionSlot("B",film("B"),1,ottOffers=listOf(open,rejected))
            assertTrue(CareerStore(context).write(Save(pending=film("A"),ottDeal=signed,slots=ProductionSlots(listOf(parked)))))
            val restored = CareerStore(context).load()
            assertEquals(signed,restored.ottDeal)
            assertTrue(restored.ottOffers.isEmpty())
            assertNull(restored.slots.movies.single().ottDeal)
            assertEquals(listOf(open,rejected),restored.slots.movies.single().ottOffers)
        } finally {
            val editor = prefs.edit()
            if (original == null) editor.remove("career") else editor.putString("career", original)
            editor.commit()
        }
    }
}
