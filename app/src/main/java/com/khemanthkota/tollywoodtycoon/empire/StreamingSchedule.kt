package com.khemanthkota.tollywoodtycoon.empire

internal data class StreamingPayment(val film: String, val buyer: String, val dueDay: Int, val amount: Int) {
    init { require(dueDay >= 0 && amount >= 0) }
    companion object {
        fun from(film: String, offer: RightsOffer, actualReleaseDay: Int, gross: Int): StreamingPayment {
            require(actualReleaseDay >= 0)
            return StreamingPayment(film, offer.buyer, Math.addExact(actualReleaseDay,offer.theatricalWindowDays), Math.toIntExact(offer.revenue(gross)))
        }
    }
}
internal data class StreamingSettlement(val remaining: List<StreamingPayment>, val received: Int)
internal fun settleStreaming(payments: List<StreamingPayment>, day: Int): StreamingSettlement {
    val (due, remaining) = payments.partition { it.dueDay <= day }
    return StreamingSettlement(remaining,Math.toIntExact(due.sumOf { it.amount.toLong() }))
}
