package com.khemanthkota.tollywoodtycoon.empire

internal object PostReleasePricing {
    fun percentage(film: FilmResult): Int {
        val recovery = (film.revenue.toLong()-film.rights)*100 / film.cost.coerceAtLeast(1)
        return when {
            recovery < 25 -> 5
            recovery < 60 -> 18
            recovery < 100 -> 35
            recovery < 150 -> 62
            recovery < 200 -> 100
            recovery < 300 -> 150
            else -> 215
        }
    }
    fun price(film: FilmResult, age: Int): Int {
        require(age >= 0)
        return (film.cost.toLong()*percentage(film)*(100-age.coerceAtMost(90)/2)/10000)
            .coerceIn(1,Int.MAX_VALUE.toLong()/2).toInt()
    }
}
