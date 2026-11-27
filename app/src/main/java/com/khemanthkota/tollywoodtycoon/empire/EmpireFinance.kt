package com.khemanthkota.tollywoodtycoon.empire

data class StudioLoan(val id:String,val principal:Int,val outstanding:Int,val annualRateBps:Int,val monthsRemaining:Int) {
    init { require(principal>0 && outstanding in 0..principal && annualRateBps>=0 && monthsRemaining>=0) }
}
data class FinanceLedger(val cash:Int,val loans:List<StudioLoan> = emptyList(),val overdue:Int=0) {
    fun borrow(id:String,amount:Int,months:Int,credibility:Int):FinanceLedger {
        require(id !in loans.map { it.id } && months in 3..60 && credibility in 0..100 && credibility>=40)
        val limit=(cash.coerceAtLeast(0).toLong()*credibility/100-loans.sumOf { it.outstanding.toLong() }).coerceAtLeast(0)
        require(amount>0 && amount<=limit && cash.toLong()+amount<=Int.MAX_VALUE)
        return copy(cash=cash+amount,loans=loans+StudioLoan(id,amount,amount,1800-credibility*10,months))
    }
    fun month():FinanceLedger {
        var balance=cash
        var arrears=overdue
        val next=loans.map { loan ->
            if(loan.outstanding==0) loan else {
                val principal=(loan.outstanding.toLong()+loan.monthsRemaining.coerceAtLeast(1)-1)/loan.monthsRemaining.coerceAtLeast(1)
                val interest=(loan.outstanding.toLong()*loan.annualRateBps+119999)/120000
                val paid=minOf(balance.coerceAtLeast(0).toLong(),principal+interest)
                balance-=paid.toInt()
                arrears+=(interest-paid).coerceAtLeast(0).toInt()
                loan.copy(outstanding=loan.outstanding-(paid-interest).coerceAtLeast(0).toInt(),monthsRemaining=(loan.monthsRemaining-1).coerceAtLeast(0))
            }
        }
        return copy(cash=balance,loans=next.filter { it.outstanding>0 },overdue=arrears)
    }
}
