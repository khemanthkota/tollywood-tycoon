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

internal object ReleaseSeasonPlanner {
    // Fictional 365-day game years, with approximate festival seasons rather than civil dates.
    fun year(year: Int, region: String, seed: Int): ReleaseMarketCalendar {
        require(year in 0..(Int.MAX_VALUE/365-1) && region.isNotBlank())
        val offset = year*365
        val holidays = mutableListOf<MarketHoliday>()
        fun period(start: Int, duration: Int, name: String, boost: Double) {
            repeat(duration) { holidays.add(MarketHoliday(offset+start+it,name,region,boost)) }
        }
        period(25,3,"Republic Day season",.25)
        period(120,14,"Summer vacation",.20)
        period(350,10,"Year-end holidays",.35)
        if(region.equals("Telugu",true) || region.equals("Tamil",true)) period(12,5,"Harvest festival season",.65)
        period(280,5,"Dussehra season",.45)
        period(305,5,"Diwali season",.55)
        val rng = kotlin.random.Random(seed xor year xor region.hashCode())
        val peakDays = holidays.map { it.day }.distinct()
        val rivals = (0 until 24).map { index ->
            val day = if(index%3==0) peakDays[rng.nextInt(peakDays.size)] else offset+rng.nextInt(365)
            AnnouncedRelease("$year-$region-$index","Rival film ${index+1}",day,region,rng.nextInt(30,96),rng.nextInt(5,31))
        }
        return ReleaseMarketCalendar(holidays,rivals)
    }
}

internal data class ReleaseCampaign(val releaseDay: Int,val marketingLakh: Int = 0,val postponementLakh: Long = 0,val wastedMarketingLakh: Int = 0) {
    init { require(releaseDay >= 0 && marketingLakh >= 0 && postponementLakh >= 0 && wastedMarketingLakh in 0..marketingLakh) }
    fun postpone(newDay: Int, dailyHoldingLakh: Int, availableLakh: Long): ReleaseCampaign {
        require(newDay > releaseDay && dailyHoldingLakh >= 0)
        val holding = (newDay.toLong()-releaseDay)*dailyHoldingLakh
        val wasted = (marketingLakh-wastedMarketingLakh)/5
        require(holding <= availableLakh && holding <= Long.MAX_VALUE-postponementLakh)
        return copy(releaseDay=newDay,postponementLakh=postponementLakh+holding,wastedMarketingLakh=wastedMarketingLakh+wasted)
    }
    fun promote(extraLakh: Int, availableLakh: Long): ReleaseCampaign {
        require(extraLakh >= 0 && extraLakh <= availableLakh && extraLakh <= Int.MAX_VALUE-marketingLakh)
        return copy(marketingLakh=marketingLakh+extraLakh)
    }
    val demandBoost: Double get() = 1.0 + .35*(1.0-kotlin.math.exp(-(marketingLakh-wastedMarketingLakh)/100.0))
}
