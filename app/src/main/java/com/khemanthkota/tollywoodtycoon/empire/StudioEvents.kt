package com.khemanthkota.tollywoodtycoon.empire

internal object StudioEvents {
    fun rivalMonth(rivals: List<RivalStudio>, day: Int, seed: Int): List<RivalStudio> = rivals.map { rival ->
        val rng = kotlin.random.Random(seed xor day xor rival.name.hashCode())
        val budget = (rival.cash.coerceAtLeast(0).toLong() * (10+rival.ambition.coerceIn(0,100)/5)/100).coerceAtMost(500)
        val revenue = budget * rng.nextInt(40,181)/100
        rival.copy(cash=(rival.cash.toLong()-budget+revenue).coerceIn(0,Int.MAX_VALUE.toLong()).toInt(),
            preferredGenre=if(rng.nextInt(100)<20) Genre.values().random(rng) else rival.preferredGenre)
    }
    fun birthdays(day: Int): List<Talent> = Talent.values().filter { (day % 365) == (35 + it.ordinal * 97) % 365 }
    fun occupancy(base: Double, ownFilm: Boolean, rivalCount: Int): Int =
        ((base*100).toInt() + if(ownFilm) 10 else 0).plus(rivalCount.coerceIn(0,3)*3).coerceIn(0,100)
    fun distressed(cash: Int, arrears: Long): Boolean = arrears > cash.coerceAtLeast(0).toLong()
    fun resale(theater: StudioTheater): Int = if(theater.acquisition == TheaterAcquisition.LEASE) 0 else theater.acquisition.upfront*60/100
    fun managerGift(cash: Int, reserved: Long, trust: Int): Boolean = trust < 65 && cash.toLong()-reserved >= 100
}
