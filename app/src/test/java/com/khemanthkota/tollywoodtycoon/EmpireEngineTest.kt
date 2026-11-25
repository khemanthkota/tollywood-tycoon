package com.khemanthkota.tollywoodtycoon

import com.khemanthkota.tollywoodtycoon.empire.*
import org.junit.Test
import org.junit.Assert.*

class EmpireEngineTest {
    private val plan = FilmPlan("First Film", Genre.ACTION, Talent.NEWCOMER, Director.DEBUT, 200)
    @Test fun firstFilmUpdatesCareer() {
        val c = Career()
        val p = EmpireEngine.start(c, plan)
        val (next, result) = EmpireEngine.finish(c, p)
        assertEquals(1, next.completed)
        assertEquals(c.cash + result.profit, next.cash)
        assertEquals(result.revenue - result.cost, result.profit)
        assertFalse(p.event)
    }
    @Test(expected = IllegalArgumentException::class)
    fun cannotOverspend() {
        EmpireEngine.start(Career(), plan.copy(budget = 1001))
    }
    @Test fun repeatableSimulation() {
        val c = Career()
        assertEquals(EmpireEngine.finish(c, EmpireEngine.start(c, plan)),
            EmpireEngine.finish(c, EmpireEngine.start(c, plan)))
    }
    @Test fun progressionUnlocks() {
        assertEquals(1, Career().unlocked)
        assertEquals(2, Career(completed = 2).unlocked)
        assertEquals(5, Career(completed = 15).unlocked)
    }
    @Test fun compromiseChangesCostAndQuality() {
        val c = Career(completed = 3)
        val p = Production(plan, true, 123)
        val repair = EmpireEngine.finish(c, p, Response.REPAIR).second
        val compromise = EmpireEngine.finish(c, p, Response.COMPROMISE).second
        assertEquals(15, repair.cost - compromise.cost)
        assertTrue(repair.quality >= compromise.quality)
    }
}
