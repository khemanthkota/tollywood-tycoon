package com.khemanthkota.tollywoodtycoon.empire

data class TalentOffer(val studio:String,val fee:Int,val advance:Int,val films:Int,val exclusive:Boolean)
object EmpireTalentMarket {
    fun choose(offers:List<TalentOffer>,balances:Map<String,Int>,loyalStudio:String?=null):TalentOffer? =
        offers.filter { it.films>0 && it.fee>=0 && it.advance>=0 && it.advance.toLong()+it.fee <= (balances[it.studio]?:0) }
            .maxWithOrNull(compareBy<TalentOffer> { it.advance.toLong()+it.fee.toLong()*it.films + if(it.studio==loyalStudio) it.fee/5 else 0 }.thenBy { it.studio })
    fun rivalOffer(rival:RivalStudio,fee:Int):TalentOffer? {
        require(fee>0)
        val offer=fee*(100+rival.ambition)/100
        return if(rival.cash>=offer+100) TalentOffer(rival.name,offer,0,1,false) else null
    }
}
