package com.khemanthkota.tollywoodtycoon.empire

enum class Department { CREATIVE, PRODUCTION, TECHNICAL, MARKETING, FINANCE, TALENT }
enum class Delegation { MANUAL, ASSISTED, AUTOMATIC, EXCEPTION_ONLY }
data class StudioManager(val id:String,val department:Department,val skill:Int,val reliability:Int,val salary:Int,val experience:Int=0) {
    init { require(skill in 0..100 && reliability in 0..100 && salary>0 && experience>=0) }
}
data class ManagerPolicy(val mode:Delegation,val spendingLimit:Int) { init { require(spendingLimit>=0) } }
data class ManagerReport(val choices:Map<FilmCraft,CraftInvestment>,val cost:Int,val explanation:String,val needsApproval:Boolean)
object EmpireManagers {
    fun department(craft:FilmCraft):Department=when(craft.stage) {
        CraftStage.PRE_PRODUCTION -> when(craft) {
            FilmCraft.CASTING -> Department.TALENT
            FilmCraft.WRITING,FilmCraft.DIRECTION -> Department.CREATIVE
            else -> Department.PRODUCTION
        }
        CraftStage.SHOOTING -> Department.PRODUCTION
        CraftStage.POST_PRODUCTION -> Department.TECHNICAL
        CraftStage.RELEASE -> Department.MARKETING
    }
    fun recommend(manager:StudioManager,policy:ManagerPolicy,crafts:List<FilmCraft>,cash:Int):ManagerReport {
        val limit=minOf(policy.spendingLimit,cash.coerceAtLeast(0))
        var spent=0
        val choices=linkedMapOf<FilmCraft,CraftInvestment>()
        if(policy.mode!=Delegation.MANUAL) crafts.filter { department(it)==manager.department }.sortedByDescending { it.weight }.forEach { craft ->
            val investment=if(manager.skill>=80 && manager.reliability>=70 && spent+CraftInvestment.PREMIUM.cost<=limit) CraftInvestment.PREMIUM
                else if(manager.skill>=50 && spent+CraftInvestment.STANDARD.cost<=limit) CraftInvestment.STANDARD else null
            if(investment!=null) { choices[craft]=investment; spent+=investment.cost }
        }
        val approval=policy.mode==Delegation.ASSISTED || (policy.mode==Delegation.EXCEPTION_ONLY && manager.reliability<70)
        return ManagerReport(choices,spent,"Prioritized higher-weight crafts within the approved spending limit.",approval)
    }
    fun train(manager:StudioManager,cash:Int):Pair<StudioManager,Int> {
        val cost=manager.salary*2
        require(cash>=cost)
        return manager.copy(skill=(manager.skill+3).coerceAtMost(100),experience=manager.experience+1) to (cash-cost)
    }
}
