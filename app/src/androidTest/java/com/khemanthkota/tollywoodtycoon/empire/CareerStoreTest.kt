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
}
