package com.khemanthkota.tollywoodtycoon.empire

internal object PostReleaseStreaming {
    fun offers(film: FilmResult, releaseDay: Int, currentDay: Int, alreadySold: Boolean): List<RightsOffer> {
        require(releaseDay >= 0 && currentDay >= releaseDay)
        if (alreadySold || film.gross <= 0 || currentDay-releaseDay !in 7..90) return emptyList()
        val age = currentDay-releaseDay
        val base = ((film.gross.toLong()*25/100 + film.quality)* (100-age/2)/100).coerceIn(1,Int.MAX_VALUE.toLong()/2).toInt()
        return listOf(
            RightsOffer("StreamVista",RightsCategory.OTT,base,theatricalWindowDays=56),
            RightsOffer("CinemaPlus",RightsCategory.OTT,base*85/100,theatricalWindowDays=28)
        )
    }
    fun payment(film: FilmResult, offer: RightsOffer, releaseDay: Int, acceptedDay: Int): StreamingPayment {
        require(acceptedDay >= releaseDay && !offer.directStreaming && offer.category == RightsCategory.OTT)
        val scheduled = StreamingPayment.from(film.title,offer,releaseDay,film.gross)
        return scheduled.copy(dueDay=maxOf(acceptedDay,scheduled.dueDay))
    }
}
