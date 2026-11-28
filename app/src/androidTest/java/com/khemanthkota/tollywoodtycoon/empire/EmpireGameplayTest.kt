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
            compose.onNodeWithText("Complete Pre-production").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("10×").performClick()
            fun awaitDay(day: Int) {
                compose.waitUntil(timeoutMillis = 120000) {
                    val state = CareerStore(context).load()
                    val movie = state.pending
                    val event = movie?.eventLedger?.pendingId?.let(EmpireEventCatalog::find)
                    if (event != null) {
                        val choice = event.choices.minByOrNull { it.cost }!!
                        val node = compose.onAllNodesWithText(choice.label, substring = true)
                        if (node.fetchSemanticsNodes().size == 1) node[0].performClick()
                        false
                    } else if (state.stage == 1 && movie?.event == true && state.response == null && state.gameDay >= state.stageReadyDay) {
                        val node = compose.onAllNodesWithText("COMPROMISE •", substring = true)
                        if (node.fetchSemanticsNodes().size == 1) node[0].performClick()
                        false
                    } else {
                        state.gameDay >= maxOf(day, state.stageReadyDay)
                    }
                }
                compose.waitForIdle()
            }
            awaitDay(CareerStore(context).load().stageReadyDay)
            tap("Complete Pre-production")
            awaitDay(CareerStore(context).load().stageReadyDay)
            if (CareerStore(context).load().stage == 1) tap("Complete Principal photography")
            awaitDay(CareerStore(context).load().stageReadyDay)
            tap("Complete Post-production")
            awaitDay(CareerStore(context).load().stageReadyDay)
            compose.onNodeWithText("Pause").performClick()
            compose.onNodeWithTag("earliest-release").performScrollTo().performClick()
            compose.waitForIdle()
            assertNotNull(CareerStore(context).load().releaseDay)
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

    @Test fun parkedMovieRetainsSchedule() {
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
            compose.onNodeWithText("Complete Pre-production").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("10×").performClick()
            fun awaitDay(day: Int) {
                compose.waitUntil(timeoutMillis = 120000) {
                    val state = CareerStore(context).load()
                    val movie = state.pending
                    val event = movie?.eventLedger?.pendingId?.let(EmpireEventCatalog::find)
                    if (event != null) {
                        val choice = event.choices.minByOrNull { it.cost }!!
                        val node = compose.onAllNodesWithText(choice.label, substring = true)
                        if (node.fetchSemanticsNodes().size == 1) node[0].performClick()
                        false
                    } else if (state.stage == 1 && movie?.event == true && state.response == null && state.gameDay >= state.stageReadyDay) {
                        val node = compose.onAllNodesWithText("COMPROMISE •", substring = true)
                        if (node.fetchSemanticsNodes().size == 1) node[0].performClick()
                        false
                    } else {
                        state.gameDay >= maxOf(day, state.stageReadyDay)
                    }
                }
                compose.waitForIdle()
            }
            awaitDay(CareerStore(context).load().stageReadyDay)
            tap("Complete Pre-production")
            awaitDay(CareerStore(context).load().stageReadyDay)
            if (CareerStore(context).load().stage == 1) tap("Complete Principal photography")
            awaitDay(CareerStore(context).load().stageReadyDay)
            tap("Complete Post-production")
            awaitDay(CareerStore(context).load().stageReadyDay)
            compose.onNodeWithText("Pause").performClick()
            compose.onNodeWithTag("earliest-release").performScrollTo().performClick()
            compose.waitForIdle()
            assertNotNull(CareerStore(context).load().releaseDay)
            val beforePark = CareerStore(context).load()
            tap("Park movie / start another")
            val parked = CareerStore(context).load().slots.movies.single()
            assertEquals(beforePark.stageReadyDay, parked.stageReadyDay)
            assertEquals(beforePark.releaseDay, parked.releaseDay)
            assertEquals(beforePark.stage, parked.stage)
            assertEquals("Smoke Test Film", parked.production.plan.title)
            assertNull(CareerStore(context).load().pending)
            compose.waitForIdle()
            val saved = CareerStore(context).load()
            assertEquals(0, saved.career.completed)
            assertNull(saved.pending)
        } finally {
            val editor = prefs.edit()
            if (original == null) editor.remove("career") else editor.putString("career", original)
            editor.commit()
        }
    }
}
