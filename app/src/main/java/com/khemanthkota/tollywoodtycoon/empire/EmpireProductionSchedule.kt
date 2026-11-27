package com.khemanthkota.tollywoodtycoon.empire

data class FilmBooking(val id:String,val studio:String,val start:Int,val end:Int,val resources:Set<String>,val reservedBudget:Int) {
    init { require(id.isNotBlank() && end>start && reservedBudget>0) }
}
data class ProductionSchedule(val bookings:List<FilmBooking> = emptyList()) {
    fun available(resource:String,start:Int,end:Int)=bookings.none { resource in it.resources && start<it.end && end>it.start }
    fun reserve(booking:FilmBooking,cash:Int):ProductionSchedule {
        require(bookings.none { it.id==booking.id })
        require(booking.resources.all { available(it,booking.start,booking.end) })
        require(bookings.filter { it.studio==booking.studio }.sumOf { it.reservedBudget.toLong() }+booking.reservedBudget<=cash)
        return copy(bookings=bookings+booking)
    }
    fun release(id:String)=copy(bookings=bookings.filterNot { it.id==id })
}
