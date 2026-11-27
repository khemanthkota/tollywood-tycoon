package com.khemanthkota.tollywoodtycoon.empire

data class ActiveFilm(val id:String,val production:Production,val booking:FilmBooking,val stage:Int=0)
data class FilmPortfolio(val career:Career,val active:List<ActiveFilm> = emptyList(),val history:List<FilmResult> = emptyList()) {
    val reserved get()=active.sumOf { it.booking.reservedBudget }
    val availableCash get()=career.cash-reserved
    fun launch(id:String,plan:FilmPlan,day:Int,duration:Int,resources:Set<String>,economy:AssetEconomy?=null):FilmPortfolio {
        require(active.size < if(career.completed>=15) 3 else if(career.completed>=8) 2 else 1)
        val booking=FilmBooking(id,"player",day,day+duration,resources,plan.budget+30)
        ProductionSchedule(active.map { it.booking }).reserve(booking,career.cash)
        val production=EmpireEngine.start(career.copy(cash=availableCash,seed=career.seed xor id.hashCode()),plan,economy,startDay=day,endDay=day+duration)
        return copy(active=active+ActiveFilm(id,production,booking))
    }
    fun advance(id:String):FilmPortfolio {
        val film=active.single { it.id==id }
        require(film.stage<3)
        return copy(active=active.map { if(it.id==id) it.copy(stage=it.stage+1) else it })
    }
    fun release(id:String,day:Int,response:Response?=null):FilmPortfolio {
        val film=active.single { it.id==id }
        require(film.stage==3 && day>=film.booking.end)
        val (next,result)=EmpireEngine.finish(career,film.production,response)
        require(next.cash>=reserved-film.booking.reservedBudget)
        return copy(career=next,active=active.filterNot { it.id==id },history=history+result)
    }
}
