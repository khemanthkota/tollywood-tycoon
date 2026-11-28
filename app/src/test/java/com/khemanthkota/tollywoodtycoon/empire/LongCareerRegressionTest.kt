package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Test
import org.junit.Assert.*

class LongCareerRegressionTest {
    @Test fun threeYearsOfPayrollAndTax() {
        var cash=100000
        var accounts=BusinessAccounts()
        var rivals=EmpireMarket.initialRivals
        for(day in 1..1095) {
            val next=BusinessCalendar.closeDay(day,cash,accounts,100)
            cash=next.cash
            accounts=next.accounts
            if(BusinessCalendar.monthBoundary(day)) rivals=StudioEvents.rivalMonth(rivals,day,123)
        }
        assertEquals(100000-3*1480,cash)
        assertEquals(0L,accounts.arrears)
        assertEquals(0L,accounts.lastTax)
        assertTrue(rivals.all { it.cash>=0 })
    }
}
