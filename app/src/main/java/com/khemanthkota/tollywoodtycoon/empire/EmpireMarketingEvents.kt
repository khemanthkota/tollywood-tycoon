package com.khemanthkota.tollywoodtycoon.empire

object EmpireMarketingEvents {
    val catalog = listOf(
        ProductionEvent("market_1", EventStage.MARKETING, "Trailer feedback", listOf(EventChoice("invest", "Allocate additional resources", 6, 1, 0), EventChoice("adapt", "Adjust the plan", 2, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_2", EventStage.MARKETING, "Poster reveal", listOf(EventChoice("invest", "Allocate additional resources", 7, 2, 1), EventChoice("adapt", "Adjust the plan", 3, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_3", EventStage.MARKETING, "Viral interview", listOf(EventChoice("invest", "Allocate additional resources", 8, 3, 2), EventChoice("adapt", "Adjust the plan", 4, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_4", EventStage.MARKETING, "Press scheduling", listOf(EventChoice("invest", "Allocate additional resources", 9, 4, 0), EventChoice("adapt", "Adjust the plan", 5, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_5", EventStage.MARKETING, "Social media backlash", listOf(EventChoice("invest", "Allocate additional resources", 10, 1, 1), EventChoice("adapt", "Adjust the plan", 2, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_6", EventStage.MARKETING, "Fan campaign", listOf(EventChoice("invest", "Allocate additional resources", 11, 2, 2), EventChoice("adapt", "Adjust the plan", 3, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_7", EventStage.MARKETING, "Music launch", listOf(EventChoice("invest", "Allocate additional resources", 12, 3, 0), EventChoice("adapt", "Adjust the plan", 4, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_8", EventStage.MARKETING, "Brand partnership", listOf(EventChoice("invest", "Allocate additional resources", 13, 4, 1), EventChoice("adapt", "Adjust the plan", 5, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_9", EventStage.MARKETING, "Promotional tour", listOf(EventChoice("invest", "Allocate additional resources", 14, 1, 2), EventChoice("adapt", "Adjust the plan", 2, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_10", EventStage.MARKETING, "Festival promotion", listOf(EventChoice("invest", "Allocate additional resources", 6, 2, 0), EventChoice("adapt", "Adjust the plan", 3, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_11", EventStage.MARKETING, "Competitor trailer", listOf(EventChoice("invest", "Allocate additional resources", 7, 3, 1), EventChoice("adapt", "Adjust the plan", 4, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_12", EventStage.MARKETING, "Influencer opportunity", listOf(EventChoice("invest", "Allocate additional resources", 8, 4, 2), EventChoice("adapt", "Adjust the plan", 5, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_13", EventStage.MARKETING, "Merchandise demand", listOf(EventChoice("invest", "Allocate additional resources", 9, 1, 0), EventChoice("adapt", "Adjust the plan", 2, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_14", EventStage.MARKETING, "Teaser leak", listOf(EventChoice("invest", "Allocate additional resources", 10, 2, 1), EventChoice("adapt", "Adjust the plan", 3, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("market_15", EventStage.MARKETING, "Audience survey", listOf(EventChoice("invest", "Allocate additional resources", 11, 3, 2), EventChoice("adapt", "Adjust the plan", 4, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0)))
    )
}
