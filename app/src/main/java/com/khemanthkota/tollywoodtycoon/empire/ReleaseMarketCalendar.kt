package com.khemanthkota.tollywoodtycoon.empire

internal data class MarketHoliday(val day: Int,val name: String,val region: String,val demandBoost: Double) {
    init { require(day >= 0 && name.isNotBlank() && region.isNotBlank() && demandBoost in 0.0..1.0) }
}
internal data class AnnouncedRelease(val id: String,val title: String,val day: Int,val region: String,val appeal: Int,val requestedScreens: Int) {
    init { require(id.isNotBlank() && day >= 0 && appeal in 0..100 && requestedScreens > 0) }
}
internal data class ReleaseForecast(val day: Int,val holidays: List<String>,val rivals: List<AnnouncedRelease>,val demandMultiplier: Double,val availableScreens: Int,val occupancy: Double)
internal data class ReleaseMarketCalendar(val holidays: List<MarketHoliday> = emptyList(),val releases: List<AnnouncedRelease> = emptyList(),val startWeekday: Int = 0) {
    init { require(startWeekday in 0..6) }
    fun forecast(day: Int,region: String,appeal: Int,totalScreens: Int): ReleaseForecast {
        require(day >= 0 && appeal in 0..100 && totalScreens >= 0)
        val weekday = (day.toLong()+startWeekday)%7
        val events = holidays.filter { it.day == day && (it.region == region || it.region == "ALL") }
        val weekend = when(weekday) { 4L -> .15; 5L,6L -> .35; else -> 0.0 }
        val boost = 1.0+weekend+events.sumOf { it.demandBoost }.coerceAtMost(1.0)
        val rivals = releases.filter { it.region == region && day.toLong()-it.day in 0L..6L }
        val free = (totalScreens.toLong()-rivals.sumOf { it.requestedScreens.toLong() }).coerceAtLeast(0).toInt()
        val competition = 1.0+rivals.sumOf { it.appeal/100.0 }*.35
        val occupancy = if(free == 0) 0.0 else ((.15+appeal*.006)*boost/competition).coerceIn(0.0,1.0)
        return ReleaseForecast(day,events.map { it.name },rivals,boost,free,occupancy)
    }
    fun dailyAudience(openingOccupancy: Double,quality: Int,daysSinceRelease: Int): Double {
        require(openingOccupancy in 0.0..1.0 && quality in 0..100 && daysSinceRelease >= 0)
        val retention = .45+quality*.005
        return (openingOccupancy*Math.pow(retention,daysSinceRelease.toDouble()/3)).coerceIn(0.0,1.0)
    }
}
