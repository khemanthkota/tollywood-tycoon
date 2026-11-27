package com.khemanthkota.tollywoodtycoon.empire

// Ticket and concession prices are rupees; business totals are lakh rupees.
internal data class OwnedTheater(val id: String,val location: String,val screens: Int,val seatsPerScreen: Int,val purchaseLakh: Double,val dailyOperatingLakh: Double) {
    init { require(id.isNotBlank() && location.isNotBlank() && screens > 0 && seatsPerScreen > 0); require(listOf(purchaseLakh,dailyOperatingLakh).all { it.isFinite() && it >= 0 }) }
}
internal data class Screening(val filmId: String,val ownFilm: Boolean,val days: Int,val showsPerScreen: Int,val occupancy: Double,val ticketRupees: Double,val concessionRupees: Double,val concessionMargin: Double,val distributorShare: Double) {
    init { require(filmId.isNotBlank() && days > 0 && showsPerScreen > 0); require(listOf(occupancy,concessionMargin,distributorShare).all { it in 0.0..1.0 }); require(listOf(ticketRupees,concessionRupees).all { it.isFinite() && it >= 0 }) }
}
internal data class ExhibitionResult(val ticketGross: Double,val concessionProfit: Double,val distributorPayment: Double,val operatingCost: Double,val internalTransfer: Double) {
    val theaterProfit get() = ticketGross + concessionProfit - distributorPayment - internalTransfer - operatingCost
    // Own-film distributor receipts are internal and must be eliminated on consolidation.
    val consolidatedContribution get() = theaterProfit + internalTransfer
}
internal object TheaterBusiness {
    fun screen(theater: OwnedTheater, booking: Screening): ExhibitionResult {
        val admissions = theater.screens.toDouble()*theater.seatsPerScreen*booking.days*booking.showsPerScreen*booking.occupancy
        val gross = admissions*booking.ticketRupees/100000
        val concession = admissions*booking.concessionRupees*booking.concessionMargin/100000
        val share = gross*booking.distributorShare
        return ExhibitionResult(gross,concession,if(booking.ownFilm) 0.0 else share,theater.dailyOperatingLakh*booking.days,if(booking.ownFilm) share else 0.0)
    }
}

internal data class TheaterPortfolio(val theaters: List<OwnedTheater> = emptyList()) {
    init { require(theaters.map { it.id }.distinct().size == theaters.size) }
    fun purchase(theater: OwnedTheater, cashLakh: Double, reservedLakh: Double): Pair<TheaterPortfolio, Double> {
        require(cashLakh.isFinite() && reservedLakh.isFinite() && reservedLakh >= 0)
        require(theaters.none { it.id == theater.id })
        require(theater.purchaseLakh <= cashLakh - reservedLakh) { "Insufficient unreserved cash" }
        return copy(theaters = theaters + theater) to (cashLakh - theater.purchaseLakh)
    }
    fun upgrade(id: String, extraSeats: Int, costLakh: Double, cashLakh: Double, reservedLakh: Double): Pair<TheaterPortfolio, Double> {
        require(extraSeats > 0 && listOf(costLakh,cashLakh,reservedLakh).all { it.isFinite() && it >= 0 })
        require(costLakh <= cashLakh - reservedLakh)
        val old = theaters.first { it.id == id }
        require(extraSeats <= Int.MAX_VALUE - old.seatsPerScreen)
        val upgraded = old.copy(seatsPerScreen = old.seatsPerScreen + extraSeats, purchaseLakh = old.purchaseLakh + costLakh)
        return copy(theaters = theaters.map { if(it.id == id) upgraded else it }) to (cashLakh-costLakh)
    }
}

internal data class ScreeningContract(val id: String,val theaterId: String,val startDay: Int,val screening: Screening) {
    init { require(id.isNotBlank() && theaterId.isNotBlank() && startDay >= 0 && screening.days <= Int.MAX_VALUE-startDay) }
    val endDay get() = startDay + screening.days
}
internal data class ExhibitionCalendar(val contracts: List<ScreeningContract> = emptyList(),val settledThrough: Int = 0) {
    init { require(settledThrough >= 0) }
    fun book(contract: ScreeningContract, portfolio: TheaterPortfolio): ExhibitionCalendar {
        require(portfolio.theaters.any { it.id == contract.theaterId })
        require(contract.startDay >= settledThrough)
        require(contracts.none { it.id == contract.id || (it.theaterId == contract.theaterId && it.startDay < contract.endDay && contract.startDay < it.endDay) })
        return copy(contracts = contracts + contract)
    }
    fun settle(day: Int, portfolio: TheaterPortfolio): Pair<ExhibitionCalendar,Double> {
        require(day >= settledThrough)
        val earnings = portfolio.theaters.sumOf { theater ->
            val active = contracts.filter { it.theaterId == theater.id }
            val screenedDays = active.sumOf { (minOf(day,it.endDay)-maxOf(settledThrough,it.startDay)).coerceAtLeast(0) }
            val revenue = active.sumOf { contract ->
                val days = (minOf(day,contract.endDay)-maxOf(settledThrough,contract.startDay)).coerceAtLeast(0)
                if(days == 0) 0.0 else TheaterBusiness.screen(theater,contract.screening.copy(days=days)).theaterProfit
            }
            revenue - (day-settledThrough-screenedDays)*theater.dailyOperatingLakh
        }
        return copy(settledThrough=day) to earnings
    }
}

internal object TheaterDemand {
    fun occupancy(popularity: Int, regionalAppeal: Double, ticketPrice: Double, referencePrice: Double, competingReleases: Int, daysSinceRelease: Int): Double {
        require(popularity in 0..100 && regionalAppeal in 0.0..2.0 && ticketPrice.isFinite() && ticketPrice >= 0 && referencePrice.isFinite() && referencePrice > 0 && competingReleases >= 0 && daysSinceRelease >= 0)
        val priceFactor = (referencePrice / maxOf(ticketPrice, referencePrice * .5)).coerceIn(.2,1.4)
        val competition = 1.0 / (1.0 + competingReleases * .15)
        val decay = 1.0 / (1.0 + daysSinceRelease * .045)
        return ((.05 + popularity / 100.0 * .95) * regionalAppeal * priceFactor * competition * decay).coerceIn(0.0,1.0)
    }
}
