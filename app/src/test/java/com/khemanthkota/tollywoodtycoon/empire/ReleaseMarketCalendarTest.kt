package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class ReleaseMarketCalendarTest {
    @Test fun festivalWeekendBoostsDemand() {
        val calendar = ReleaseMarketCalendar(listOf(MarketHoliday(5,"Festival","Telugu",.5)))
        assertTrue(calendar.forecast(5,"Telugu",50,100).occupancy > calendar.forecast(1,"Telugu",50,100).occupancy)
    }
    @Test fun competitionConsumesScreens() {
        val calendar = ReleaseMarketCalendar(releases=listOf(AnnouncedRelease("r","Rival",5,"Telugu",90,100)))
        val forecast = calendar.forecast(5,"Telugu",50,100)
        assertEquals(0,forecast.availableScreens)
        assertEquals(0.0,forecast.occupancy,0.0001)
    }
    @Test fun weakMoviesLoseAudienceFaster() {
        val calendar = ReleaseMarketCalendar()
        assertTrue(calendar.dailyAudience(.9,30,6) < calendar.dailyAudience(.9,90,6))
    }
}
