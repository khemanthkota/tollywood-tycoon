package com.khemanthkota.tollywoodtycoon.empire
import kotlin.random.Random
internal enum class RightsCategory { OTT, SATELLITE, MUSIC, DUBBING, REMAKE, OVERSEAS }
internal data class RightsOffer(val buyer: String, val category: RightsCategory, val guaranteedLakh: Int, val bonusLakh: Int = 0, val grossThresholdLakh: Int = 0, val theatricalWindowDays: Int = 0, val directStreaming: Boolean = false) {
    init { require(buyer.isNotBlank() && listOf(guaranteedLakh,bonusLakh,grossThresholdLakh,theatricalWindowDays).all { it >= 0 }); require(!directStreaming || category == RightsCategory.OTT && theatricalWindowDays == 0) }
    fun revenue(gross: Int): Long = guaranteedLakh.toLong() + if (gross >= grossThresholdLakh) bonusLakh else 0
}
internal data class RightsBook(val accepted: Map<RightsCategory,RightsOffer> = emptyMap()) {
    fun accept(offer: RightsOffer): RightsBook {
        require(offer.category !in accepted) { "Rights already sold" }
        return copy(accepted = accepted + (offer.category to offer))
    }
    val guaranteedRevenue: Long get() = accepted.values.sumOf { it.guaranteedLakh.toLong() }
    fun totalRevenue(gross: Int): Long = accepted.values.sumOf { it.revenue(gross) }
    val directStreaming: Boolean get() = accepted[RightsCategory.OTT]?.directStreaming == true
}
internal enum class AudienceSurprise(val multiplier: Double) { REJECTION(.35), WEAK_WORD_OF_MOUTH(.7), EXPECTED(1.0), SLEEPER_HIT(1.8), VIRAL_BLOCKBUSTER(3.0) }
internal object ReleaseSurprises {
    fun outcome(seed: Int, quality: Int): AudienceSurprise {
        require(quality in 0..100)
        val roll = Random(seed).nextInt(100)
        return when {
            roll < 12 - quality / 15 -> AudienceSurprise.REJECTION
            roll < 30 - quality / 5 -> AudienceSurprise.WEAK_WORD_OF_MOUTH
            roll >= 99 - quality / 20 -> AudienceSurprise.VIRAL_BLOCKBUSTER
            roll >= 94 - quality / 5 -> AudienceSurprise.SLEEPER_HIT
            else -> AudienceSurprise.EXPECTED
        }
    }
}

internal object StreamingMarket {
    fun offers(plan: FilmPlan, credibility: Int, seed: Int): List<RightsOffer> {
        require(credibility in 0..100)
        val rng = Random(seed)
        val base = plan.cast.appeal + plan.director.skill + credibility
        return listOf(
            RightsOffer("StreamVista", RightsCategory.OTT, base * rng.nextInt(65,91) / 100, theatricalWindowDays = 56),
            RightsOffer("CinemaPlus", RightsCategory.OTT, base * rng.nextInt(45,66) / 100, base / 2, plan.budget, 28),
            RightsOffer("PremierePlay", RightsCategory.OTT, base * rng.nextInt(95,131) / 100, directStreaming = true)
        )
    }
}

internal enum class DealStatus { OPEN, ACCEPTED, WALKED_AWAY }
internal data class RightsNegotiation(val offer: RightsOffer, val rounds: Int = 0, val status: DealStatus = DealStatus.OPEN) {
    fun counter(requestedLakh: Int, credibility: Int): RightsNegotiation {
        require(status == DealStatus.OPEN && requestedLakh >= offer.guaranteedLakh && credibility in 0..100)
        val ceiling = offer.guaranteedLakh.toLong() * (110 + credibility / 4) / 100
        if (rounds >= 2 || requestedLakh.toLong() > ceiling * 12 / 10) return copy(status = DealStatus.WALKED_AWAY)
        val revised = minOf(requestedLakh.toLong(), ceiling, Int.MAX_VALUE.toLong()).toInt()
        return copy(offer = offer.copy(guaranteedLakh = revised), rounds = rounds + 1)
    }
    fun accept(book: RightsBook): Pair<RightsNegotiation, RightsBook> {
        require(status == DealStatus.OPEN)
        return copy(status = DealStatus.ACCEPTED) to book.accept(offer)
    }
}
