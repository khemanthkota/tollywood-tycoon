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
