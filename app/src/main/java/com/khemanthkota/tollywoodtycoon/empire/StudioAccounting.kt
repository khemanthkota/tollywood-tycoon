package com.khemanthkota.tollywoodtycoon.empire

// Illustrative gameplay rates, not statutory tax or payroll advice. Amounts in lakh.
internal data class AccountingPolicy(val incomeTaxRate: Double = .25, val retirementRate: Double = .12, val gratuityRate: Double = .04, val insurancePerEmployee: Double = .02) {
    init { require(listOf(incomeTaxRate,retirementRate,gratuityRate).all { it in 0.0..1.0 }); require(insurancePerEmployee.isFinite() && insurancePerEmployee >= 0) }
}
internal data class PayrollExpense(val wages: Double, val retirement: Double, val gratuity: Double, val insurance: Double, val bonus: Double) {
    val total get() = wages + retirement + gratuity + insurance + bonus
}
internal data class AnnualAccounts(val revenue: Double, val movieCosts: Double, val payroll: Double, val gifts: Double, val overhead: Double, val interest: Double, val depreciation: Double, val previousLoss: Double = 0.0) {
    init { require(listOf(revenue,movieCosts,payroll,gifts,overhead,interest,depreciation,previousLoss).all { it.isFinite() && it >= 0 }) }
    val profitBeforeTax get() = revenue - movieCosts - payroll - gifts - overhead - interest - depreciation
    fun close(policy: AccountingPolicy): TaxSettlement {
        val taxable = (profitBeforeTax - previousLoss).coerceAtLeast(0.0)
        val tax = taxable * policy.incomeTaxRate
        return TaxSettlement(tax,profitBeforeTax-tax,(previousLoss-profitBeforeTax).coerceAtLeast(0.0))
    }
}
internal data class TaxSettlement(val tax: Double,val netProfit: Double,val carriedLoss: Double)
internal object StudioAccounting {
    fun payroll(wages: Double, employees: Int, bonus: Double, policy: AccountingPolicy): PayrollExpense {
        require(wages.isFinite() && wages >= 0 && employees >= 0 && bonus.isFinite() && bonus >= 0)
        return PayrollExpense(wages,wages*policy.retirementRate,wages*policy.gratuityRate,employees*policy.insurancePerEmployee,bonus)
    }
    fun indirectTax(outputTax: Double,inputCredit: Double): Pair<Double,Double> {
        require(listOf(outputTax,inputCredit).all { it.isFinite() && it >= 0 })
        return (outputTax-inputCredit).coerceAtLeast(0.0) to (inputCredit-outputTax).coerceAtLeast(0.0)
    }
}
