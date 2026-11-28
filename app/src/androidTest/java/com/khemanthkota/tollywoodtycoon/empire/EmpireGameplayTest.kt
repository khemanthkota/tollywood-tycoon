package com.khemanthkota.tollywoodtycoon.empire

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class EmpireGameplayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun produceAndPremiereFirstFilm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("cinema_empire_v1", 0)
        val original = prefs.getString("career", null)
        try {
            assertTrue(CareerStore(context).write(Save(career = Career(seed = 42))))
            compose.setContent { androidx.compose.material3.MaterialTheme { EmpireScreen(onBack = {}) } }
            fun tap(label: String) {
                compose.onNodeWithText(label).performScrollTo().performClick()
                compose.waitForIdle()
            }
            tap("Start / resume production")
            compose.onNodeWithText("Movie title").performScrollTo().performTextInput("Smoke Test Film")
            tap("Greenlight movie")
            tap("Complete Pre-production")
            tap("Complete Principal photography")
            tap("Complete Post-production")
            tap("Premiere movie")
            compose.waitForIdle()
            val saved = CareerStore(context).load()
            assertEquals(1, saved.career.completed)
            assertEquals("Smoke Test Film", saved.history.last().title)
            assertNull(saved.pending)
        } finally {
            val editor = prefs.edit()
            if (original == null) editor.remove("career") else editor.putString("career", original)
            editor.commit()
        }
    }
}
