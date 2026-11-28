package com.khemanthkota.tollywoodtycoon.empire

internal object PostReleaseStreaming {
    fun offers(film: FilmResult, releaseDay: Int, currentDay: Int, alreadySold: Boolean): List<RightsOffer> {
        require(releaseDay >= 0 && currentDay >= releaseDay)
        if (alreadySold || film.gross <= 0 || currentDay-releaseDay !in 7..90) return emptyList()
        val age = currentDay-releaseDay
        val round = (age-7)/14
        val rng = kotlin.random.Random(film.title.hashCode() xor releaseDay xor round)
        val viral = if(rng.nextInt(100)<8) 125 else 100
        val base = (PostReleasePricing.price(film, 7+round*14).toLong()*viral*rng.nextInt(85,116)/10000).coerceIn(1,Int.MAX_VALUE.toLong()/2).toInt()
        return listOf(
            RightsOffer("StreamVista",RightsCategory.OTT,base,theatricalWindowDays=56),
            RightsOffer("CinemaPlus",RightsCategory.OTT,base*85/100,theatricalWindowDays=28)
        )
    }
    fun counter(deal: RightsNegotiation, film: FilmResult, credibility: Int, competitors: Int, age: Int): RightsNegotiation {
        require(deal.status == DealStatus.OPEN && credibility in 0..100 && competitors >= 0 && age >= 0)
        val recovery = ((film.revenue.toLong()-film.rights)*100/film.cost.coerceAtLeast(1)).coerceIn(0,300).toInt()
        val urgency = (90-age).coerceIn(0,90)/9
        val budget = if(deal.offer.buyer=="StreamVista") 35 else 25
        val premium = (5+credibility/5+recovery/15+competitors.coerceAtMost(3)*4+urgency).coerceAtMost(budget)
        val requested = deal.offer.guaranteedLakh.toLong()*120/100
        val ceiling = deal.offer.guaranteedLakh.toLong()*(100+premium)/100
        val risk = ((20-premium).coerceAtLeast(0)*4 + deal.rounds*15).coerceAtMost(95)
        val roll = kotlin.random.Random(film.title.hashCode() xor deal.offer.buyer.hashCode() xor film.actualReleaseDay xor ((age-7)/14) xor deal.rounds).nextInt(100)
        if(deal.rounds>=2 || roll<risk) return deal.copy(status=DealStatus.WALKED_AWAY)
        return deal.copy(offer=deal.offer.copy(guaranteedLakh=minOf(requested,ceiling,Int.MAX_VALUE.toLong()).toInt()),rounds=deal.rounds+1)
    }
    fun payment(film: FilmResult, offer: RightsOffer, releaseDay: Int, acceptedDay: Int): StreamingPayment {
        require(acceptedDay >= releaseDay && !offer.directStreaming && offer.category == RightsCategory.OTT)
        val scheduled = StreamingPayment.from(film.title,offer,releaseDay,film.gross)
        return scheduled.copy(dueDay=maxOf(acceptedDay,scheduled.dueDay))
    }
}
