package com.khemanthkota.tollywoodtycoon.empire

internal data class ProductionCommitment(
    val id: String,
    val budget: Int,
    val craft: Int = 0,
    val contingency: Int = 0
) {
    init {
        require(id.isNotBlank())
        require(budget >= 0 && craft >= 0 && contingency >= 0)
    }
    val total: Long get() = budget.toLong() + craft + contingency
}

internal data class ProductionPortfolio(
    val cash: Int,
    val payroll: Int,
    val productions: List<ProductionCommitment> = emptyList()
) {
    init {
        require(cash >= 0 && payroll >= 0)
        require(productions.map { it.id }.distinct().size == productions.size)
    }
    val available: Long get() = cash.toLong() - payroll - productions.sumOf { it.total }

    fun canCommit(candidate: ProductionCommitment): Boolean =
        cash.toLong() - payroll - productions.filterNot { it.id == candidate.id }.sumOf { it.total } - candidate.total >= 0

    fun commit(candidate: ProductionCommitment): ProductionPortfolio {
        require(canCommit(candidate)) { "Insufficient uncommitted studio funds" }
        return copy(productions = productions.filterNot { it.id == candidate.id } + candidate)
    }

    fun remove(id: String): ProductionPortfolio =
        copy(productions = productions.filterNot { it.id == id })
}
