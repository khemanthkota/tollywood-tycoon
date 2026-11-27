package com.khemanthkota.tollywoodtycoon.empire

enum class ContractType { SINGLE_FILM, MULTI_FILM, EXCLUSIVE, PROFIT_SHARE }
data class TalentContract(val id:String,val talent:String,val studio:String,val type:ContractType,
    val remainingFilms:Int,val fee:Int,val advance:Int,val profitShare:Int=0,val expiresDay:Int,
    val bookedUntil:Int=0,val activeFilm:String?=null) {
    init { require(remainingFilms>=0 && fee>=0 && advance>=0 && profitShare in 0..50) }
}
data class ContractLedger(val cash:Map<String,Int>,val contracts:List<TalentContract> = emptyList()) {
    fun sign(contract:TalentContract,day:Int):ContractLedger {
        require(contract.id !in contracts.map { it.id } && contract.remainingFilms>0 && contract.expiresDay>day)
        require(contract.activeFilm==null && contract.bookedUntil<=day)
        require(contracts.none { it.talent==contract.talent && it.remainingFilms>0 && it.expiresDay>day &&
            (it.type==ContractType.EXCLUSIVE || contract.type==ContractType.EXCLUSIVE) })
        val balance=cash[contract.studio] ?: 0
        require(balance>=contract.advance)
        return copy(cash=cash+(contract.studio to balance-contract.advance),contracts=contracts+contract)
    }
    fun book(id:String,film:String,start:Int,end:Int):ContractLedger {
        val c=contracts.single { it.id==id }
        require(film.isNotBlank() && end>start && end<=c.expiresDay && c.remainingFilms>0 && c.activeFilm==null)
        require(contracts.none { it.talent==c.talent && it.activeFilm!=null && it.bookedUntil>start })
        val balance=cash[c.studio] ?: 0
        require(balance>=c.fee)
        return copy(cash=cash+(c.studio to balance-c.fee),contracts=contracts.map {
            if(it.id==id) it.copy(activeFilm=film,bookedUntil=end) else it })
    }
    fun complete(id:String,film:String,profit:Int,day:Int):ContractLedger {
        val c=contracts.single { it.id==id }
        require(c.activeFilm==film && day>=c.bookedUntil)
        val share=(profit.coerceAtLeast(0).toLong()*c.profitShare/100).toInt()
        val balance=cash[c.studio] ?: 0
        require(balance>=share)
        return copy(cash=cash+(c.studio to balance-share),contracts=contracts.map {
            if(it.id==id) it.copy(remainingFilms=it.remainingFilms-1,activeFilm=null,bookedUntil=day) else it })
    }
    fun terminate(id:String,studio:String):ContractLedger {
        val c=contracts.single { it.id==id }
        require(c.studio==studio && c.activeFilm==null)
        val penalty=c.remainingFilms*c.fee/4
        val balance=cash[studio] ?: 0
        require(balance>=penalty)
        return copy(cash=cash+(studio to balance-penalty),contracts=contracts.filterNot { it.id==id })
    }
}
