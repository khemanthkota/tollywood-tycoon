package com.khemanthkota.tollywoodtycoon.empire

object EmpirePreproductionEvents {
    private fun event(id: String, title: String, cost: Int, quality: Int, delay: Int) =
        ProductionEvent("pre_$id", EventStage.PREPRODUCTION, title, listOf(
            EventChoice("invest", "Allocate resources", cost, quality, delay),
            EventChoice("adapt", "Adapt existing plan", cost / 2, quality / 2, delay + 1),
            EventChoice("continue", "Continue without changes", 0, -2, 0)
        ))
    val catalog = listOf(
        event("script", "Script rewrite", 12, 5, 3),
        event("casting", "Lead casting conflict", 15, 3, 2),
        event("location", "Location permit delay", 8, 0, 4),
        event("chemistry", "Unexpected cast chemistry", 5, 4, 1),
        event("storyboard", "Storyboard revision", 6, 3, 2),
        event("research", "Historical research opportunity", 7, 4, 2),
        event("costume", "Costume concept mismatch", 8, 3, 2),
        event("crew", "Experienced crew available", 12, 4, 0),
        event("schedule", "Director schedule clash", 10, 1, 3),
        event("sets", "Set design revision", 14, 4, 3),
        event("insurance", "Insurance coverage gap", 9, 0, 1),
        event("audition", "Breakthrough audition", 4, 3, 1),
        event("music", "Composer concept session", 6, 3, 1),
        event("stunt", "Stunt rehearsal requirement", 10, 3, 2),
        event("weather", "Seasonal location risk", 8, 1, 2),
        event("rights", "Story rights clearance", 12, 0, 2),
        event("table", "Table read reveals weak dialogue", 5, 4, 2),
        event("supplier", "Supplier price increase", 9, 0, 1),
        event("camera", "Camera test opportunity", 7, 3, 1),
        event("consultant", "Genre consultant available", 8, 4, 1)
    )
}
