package com.khemanthkota.tollywoodtycoon.empire

data class RivalCareer(val studio:RivalStudio,val films:Int=0,val audience:Int=20,val credibility:Int=50,val pending:Production?=null)
object EmpireAutonomousStudio {
    private fun career(r:RivalCareer,seed:Int)=Career(cash=r.studio.cash,completed=r.films,credibility=r.credibility,audience=r.audience,seed=seed,rivals=emptyList())
    fun plan(r:RivalCareer,seed:Int):RivalCareer {
        if(r.pending!=null || r.studio.cash<215) return r
        val budget=(r.studio.cash.toLong()*r.studio.ambition/100).toInt().coerceIn(115,r.studio.cash-100)
        val cast=Talent.values().filter { it.fee+Director.DEBUT.fee+60<=budget }.maxByOrNull { it.appeal } ?: return r
        val director=Director.values().filter { cast.fee+it.fee+60<=budget }.maxByOrNull { it.skill } ?: return r
        val film=FilmPlan("${r.studio.name} Production ${r.films+1}",r.studio.preferredGenre,cast,director,budget)
        return r.copy(pending=EmpireEngine.start(career(r,seed),film))
    }
    fun release(r:RivalCareer,seed:Int):Pair<RivalCareer,FilmResult>? {
        val p=r.pending ?: return null
        if(r.studio.cash<p.plan.budget) return null
        val response=if(!p.event) null else if(r.studio.cash-p.plan.budget>=30) Response.RENT else if(r.studio.cash-p.plan.budget>=15) Response.REPAIR else Response.COMPROMISE
        val (next,result)=EmpireEngine.finish(career(r,seed),p,response)
        return r.copy(studio=r.studio.copy(cash=next.cash),films=next.completed,audience=next.audience,credibility=next.credibility,pending=null) to result
    }
}
