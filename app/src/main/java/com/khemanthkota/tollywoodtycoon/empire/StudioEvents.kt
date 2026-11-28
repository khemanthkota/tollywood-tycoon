package com.khemanthkota.tollywoodtycoon.empire

internal object StudioEvents {
    fun birthdays(day: Int): List<Talent> = Talent.values().filter { (day % 365) == (35 + it.ordinal * 97) % 365 }
    fun occupancy(base: Double, ownFilm: Boolean, rivalCount: Int): Int =
        ((base*100).toInt() + if(ownFilm) 10 else 0).plus(rivalCount.coerceIn(0,3)*3).coerceIn(0,100)
    fun distressed(cash: Int, arrears: Long): Boolean = arrears > cash.coerceAtLeast(0).toLong()
    fun resale(theater: StudioTheater): Int = if(theater.acquisition == TheaterAcquisition.LEASE) 0 else theater.acquisition.upfront*60/100
    fun managerGift(cash: Int, reserved: Long, trust: Int): Boolean = trust < 65 && cash.toLong()-reserved >= 100
}
