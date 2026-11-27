package com.khemanthkota.tollywoodtycoon.empire

internal enum class CrewRole { ACTOR, ACTRESS, DIRECTOR, WRITER, EDITOR, MUSIC_DIRECTOR }
internal data class TalentBond(val trust: Int = 50, val friendship: Int = 20, val respect: Int = 50) {
    init { require(listOf(trust, friendship, respect).all { it in 0..100 }) }
    fun projectOutcome(paidOnTime: Boolean, promisesKept: Boolean): TalentBond = copy(
        trust = (trust + if (paidOnTime && promisesKept) 8 else -15).coerceIn(0,100),
        respect = (respect + if (promisesKept) 5 else -10).coerceIn(0,100))
    fun gift(appropriate: Boolean): TalentBond = copy(friendship = (friendship + if (appropriate) 3 else -2).coerceIn(0,100))
}
internal data class CrewBooking(val personId: String, val filmId: String, val start: Int, val endExclusive: Int) {
    init { require(personId.isNotBlank() && filmId.isNotBlank() && start >= 0 && endExclusive > start) }
    fun overlaps(other: CrewBooking): Boolean = personId == other.personId && start < other.endExclusive && other.start < endExclusive
}
internal enum class ExtensionDecision { ACCEPT, COUNTEROFFER, DATE_CONFLICT }
internal data class ExtensionQuote(val decision: ExtensionDecision, val requiredLakh: Long)
internal data class CrewCalendar(val bookings: List<CrewBooking> = emptyList()) {
    fun quote(extension: CrewBooking, dailyFeeLakh: Int, offeredLakh: Long, bond: TalentBond): ExtensionQuote {
        require(dailyFeeLakh >= 0 && offeredLakh >= 0)
        val base = (extension.endExclusive.toLong() - extension.start) * dailyFeeLakh
        val required = base - base / 100 * (bond.trust / 5)
        val conflict = bookings.any { it.filmId != extension.filmId && it.overlaps(extension) }
        return ExtensionQuote(if (conflict) ExtensionDecision.DATE_CONFLICT else if (offeredLakh >= required) ExtensionDecision.ACCEPT else ExtensionDecision.COUNTEROFFER, required)
    }
    fun confirm(extension: CrewBooking, dailyFeeLakh: Int, offeredLakh: Long, bond: TalentBond, availableLakh: Long): CrewCalendar {
        require(offeredLakh <= availableLakh)
        require(quote(extension,dailyFeeLakh,offeredLakh,bond).decision == ExtensionDecision.ACCEPT)
        return copy(bookings = bookings + extension)
    }
}
