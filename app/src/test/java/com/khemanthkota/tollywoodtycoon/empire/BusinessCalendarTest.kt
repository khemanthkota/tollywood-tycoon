package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Test
import org.junit.Assert.*
class BusinessCalendarTest {
 @Test fun annualAccounting() {
   var state = BusinessDay(10000,BusinessAccounts(4000))
   for(day in 1..365) state = BusinessCalendar.closeDay(day,state.cash,state.accounts,100)
   assertEquals(12,(1..365).count { BusinessCalendar.monthBoundary(it) })
   assertEquals(630L,state.accounts.lastTax)
   assertEquals(7890,state.cash)
   assertEquals(0L,state.accounts.taxableProfit)
 }
 @Test fun reservedFundsAndArrears() {
   val first = BusinessCalendar.closeDay(31,100,BusinessAccounts(),100,90)
   assertEquals(90,first.cash)
   assertEquals(105L,first.accounts.arrears)
   val second = BusinessCalendar.closeDay(32,200,first.accounts,100)
   assertEquals(95,second.cash)
   assertEquals(0L,second.accounts.arrears)
 }
}
