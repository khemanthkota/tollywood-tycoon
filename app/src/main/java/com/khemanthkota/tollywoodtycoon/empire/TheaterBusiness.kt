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
