package com.khemanthkota.tollywoodtycoon.empire

internal object IndustryNews {
    fun headlines(day: Int, career: Career, history: List<FilmResult>): List<String> {
        val forecast = ReleaseSeasonPlanner.year(day/365,"Telugu",0).forecast(day,"Telugu",60,100)
        return buildList {
            forecast.holidays.forEach { add("Holiday market: $it") }
            forecast.rivals.forEach { add("Release watch: ${it.title} • Appeal ${it.appeal}") }
            history.filter { it.actualReleaseDay >= 0 && day-it.actualReleaseDay in 0..7 }.takeLast(3).reversed().forEach {
                add("Box office: ${it.title} • ${if(it.profit >= 0) "Profitable" else "Loss-making"} • ₹${it.gross}L gross")
            }
            val market = EmpireMarket.round(career.seed xor (day/7),career.rivals)
            market.rivals.take(2).forEach { add("Industry watch: ${it.studio} targets ${it.genre.name}") }
        }.distinct()
    }
}
