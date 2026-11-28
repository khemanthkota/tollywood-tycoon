package com.khemanthkota.tollywoodtycoon.empire

internal data class ProductionSlot(
    val id: String,
    val production: Production,
    val stage: Int = 0,
    val response: Response? = null,
    val crafts: Map<FilmCraft, CraftInvestment> = emptyMap(),
    val releaseDay: Int? = null
) {
    init { require(id.isNotBlank() && stage in 0..3 && (releaseDay == null || releaseDay >= 0)) }
    fun commitment(): ProductionCommitment = ProductionCommitment(
        id, production.plan.budget, EmpireProductionCrafts.assess(crafts).extraCost,
        response?.cost ?: if (production.event) Response.values().minOf { it.cost } else 0
    )
}

internal data class ProductionSlots(
    val movies: List<ProductionSlot> = emptyList(),
    val selectedId: String? = null
) {
    init {
        require(movies.map { it.id }.distinct().size == movies.size)
        require(selectedId == null || movies.any { it.id == selectedId })
    }
    val selected: ProductionSlot? get() = movies.firstOrNull { it.id == selectedId }
    fun select(id: String): ProductionSlots = copy(selectedId = id)
    fun save(slot: ProductionSlot, cash: Int, payroll: Int): ProductionSlots {
        val portfolio = ProductionPortfolio(cash, payroll, movies.map { it.commitment() })
        require(portfolio.canCommit(slot.commitment())) { "Insufficient studio funds" }
        return copy(movies = movies.filterNot { it.id == slot.id } + slot, selectedId = slot.id)
    }
    fun remove(id: String): ProductionSlots {
        val remaining = movies.filterNot { it.id == id }
        return copy(movies = remaining, selectedId = if (selectedId == id) remaining.firstOrNull()?.id else selectedId)
    }
}
