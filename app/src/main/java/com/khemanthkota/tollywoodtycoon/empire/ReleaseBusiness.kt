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
