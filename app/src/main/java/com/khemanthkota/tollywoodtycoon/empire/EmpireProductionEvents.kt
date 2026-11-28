package com.khemanthkota.tollywoodtycoon.empire

enum class EventStage { PREPRODUCTION, SHOOTING, POSTPRODUCTION, MARKETING, DISTRIBUTION }
data class EventChoice(val id: String, val label: String, val cost: Int, val quality: Int, val delay: Int)
data class ProductionEvent(val id: String, val stage: EventStage, val title: String, val choices: List<EventChoice>)
data class EventDecision(val eventId: String, val choiceId: String, val day: Int, val cost: Int, val quality: Int, val delay: Int)
data class EventLedger(val pendingId: String? = null, val decisions: List<EventDecision> = emptyList()) {
    fun queue(event: ProductionEvent): EventLedger {
        require(pendingId == null)
        require(decisions.none { it.eventId == event.id })
        return copy(pendingId = event.id)
    }
    fun resolve(event: ProductionEvent, choiceId: String, day: Int, availableCash: Int): EventLedger {
        require(pendingId == event.id)
        require(decisions.none { it.eventId == event.id })
        val choice = event.choices.single { it.id == choiceId }
        require(choice.cost <= availableCash)
        require(day >= 0)
        return EventLedger(null, decisions + EventDecision(event.id, choice.id, day, choice.cost, choice.quality, choice.delay))
    }
    val totalCost get() = decisions.sumOf { it.cost }
    val qualityDelta get() = decisions.sumOf { it.quality }
    val delayDays get() = decisions.sumOf { it.delay }
}
