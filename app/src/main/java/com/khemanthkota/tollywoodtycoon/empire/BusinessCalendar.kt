package com.khemanthkota.tollywoodtycoon.empire

// Game currency: lakh. Twelve accounting periods per 365-day year.
internal data class BusinessAccounts(val taxableProfit: Long = 0, val arrears: Long = 0, val lastTax: Long = 0) {
    fun record(profit: Long) = copy(taxableProfit = Math.addExact(taxableProfit, profit))
}
internal data class BusinessDay(val cash: Int, val accounts: BusinessAccounts)
internal object BusinessCalendar {
    fun monthBoundary(day: Int): Boolean {
        require(day > 0)
        return day.toLong()*12/365 != (day.toLong()-1)*12/365
    }
    fun employeeCost(salaries: Int, annualBonus: Boolean): Int {
        require(salaries >= 0)
        return Math.toIntExact(salaries.toLong() + salaries.toLong()*15/100 + if(annualBonus) salaries.toLong() else 0L)
    }
    fun closeDay(day: Int, cash: Int, accounts: BusinessAccounts, salaries: Int, reserved: Long = 0): BusinessDay {
        require(day > 0 && reserved >= 0)
        val annual = day % 365 == 0
        val payroll = if(monthBoundary(day)) employeeCost(salaries,annual).toLong() else 0L
        val profit = accounts.taxableProfit - payroll
        val tax = if(annual) profit.coerceAtLeast(0)/4 else 0L
        val due = Math.addExact(accounts.arrears,Math.addExact(payroll,tax))
        val paid = minOf(due,(cash.toLong()-reserved).coerceAtLeast(0))
        return BusinessDay(Math.toIntExact(cash.toLong()-paid),BusinessAccounts(if(annual) 0 else profit,due-paid,if(annual) tax else accounts.lastTax))
    }
}
