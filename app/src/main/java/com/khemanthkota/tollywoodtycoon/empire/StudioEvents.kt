package com.khemanthkota.tollywoodtycoon.empire

internal object StudioEvents {
    fun rivalMonth(rivals: List<RivalStudio>, day: Int, seed: Int): List<RivalStudio> = rivals.map { rival ->
        val rng = kotlin.random.Random(seed xor day xor rival.name.hashCode())
        val budget = (rival.cash.coerceAtLeast(0).toLong() * (10+rival.ambition.coerceIn(0,100)/5)/100).coerceAtMost(500)
        val revenue = budget * rng.nextInt(40,181)/100
        rival.copy(cash=(rival.cash.toLong()-budget+revenue).coerceIn(0,Int.MAX_VALUE.toLong()).toInt(),
            preferredGenre=if(rng.nextInt(100)<20) Genre.values().random(rng) else rival.preferredGenre)
    }
    fun rivalBid(rivals: List<RivalStudio>, talent: Talent, day: Int, seed: Int): Int =
        rivals.maxOfOrNull { rival ->
            val rng = kotlin.random.Random(seed xor (day/7) xor rival.name.hashCode() xor talent.ordinal)
            if(rival.cash < talent.fee*3 || rng.nextInt(100)>=rival.ambition.coerceIn(0,100)) 0
            else (talent.fee.toLong() * rng.nextInt(100,151)/100).coerceAtMost(rival.cash.toLong()/3).toInt()
        } ?: 0
    fun targetDay(rival: RivalStudio, day: Int): Int {
        val candidates = (day+14..day+42)
        return candidates.maxByOrNull { d ->
            val f = ReleaseSeasonPlanner.year(d/365,"Telugu",0).forecast(d,"Telugu",60,100)
            f.occupancy*f.availableScreens + kotlin.random.Random(rival.name.hashCode() xor d).nextInt(12)
        } ?: day+14
    }
    fun birthdays(day: Int): List<Talent> = Talent.values().filter { (day % 365) == (35 + it.ordinal * 97) % 365 }
    fun occupancy(base: Double, ownFilm: Boolean, rivalCount: Int): Int =
        ((base*100).toInt() + if(ownFilm) 10 else 0).plus(rivalCount.coerceIn(0,3)*3).coerceIn(0,100)
    fun distressed(cash: Int, arrears: Long): Boolean = arrears > cash.coerceAtLeast(0).toLong()
    fun resale(theater: StudioTheater): Int = if(theater.acquisition == TheaterAcquisition.LEASE) 0 else theater.acquisition.upfront*60/100
    fun managerGift(cash: Int, reserved: Long, trust: Int): Boolean = trust < 65 && cash.toLong()-reserved >= 100
}
