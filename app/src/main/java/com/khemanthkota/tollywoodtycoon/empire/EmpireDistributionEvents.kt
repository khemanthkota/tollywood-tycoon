package com.khemanthkota.tollywoodtycoon.empire

object EmpireDistributionEvents {
    val catalog = listOf(
        ProductionEvent("dist_1", EventStage.DISTRIBUTION, "Screen allocation dispute", listOf(EventChoice("invest", "Allocate additional resources", 6, 1, 0), EventChoice("adapt", "Adjust the plan", 2, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_2", EventStage.DISTRIBUTION, "Festival screening", listOf(EventChoice("invest", "Allocate additional resources", 7, 2, 1), EventChoice("adapt", "Adjust the plan", 3, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_3", EventStage.DISTRIBUTION, "Overseas distributor offer", listOf(EventChoice("invest", "Allocate additional resources", 8, 3, 2), EventChoice("adapt", "Adjust the plan", 4, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_4", EventStage.DISTRIBUTION, "Regional dubbing demand", listOf(EventChoice("invest", "Allocate additional resources", 9, 4, 0), EventChoice("adapt", "Adjust the plan", 5, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_5", EventStage.DISTRIBUTION, "Premiere venue change", listOf(EventChoice("invest", "Allocate additional resources", 10, 1, 1), EventChoice("adapt", "Adjust the plan", 2, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_6", EventStage.DISTRIBUTION, "Ticket pricing dispute", listOf(EventChoice("invest", "Allocate additional resources", 11, 2, 2), EventChoice("adapt", "Adjust the plan", 3, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_7", EventStage.DISTRIBUTION, "Competitor release shift", listOf(EventChoice("invest", "Allocate additional resources", 12, 3, 0), EventChoice("adapt", "Adjust the plan", 4, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_8", EventStage.DISTRIBUTION, "Projection issue", listOf(EventChoice("invest", "Allocate additional resources", 13, 4, 1), EventChoice("adapt", "Adjust the plan", 5, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_9", EventStage.DISTRIBUTION, "Extra show demand", listOf(EventChoice("invest", "Allocate additional resources", 14, 1, 2), EventChoice("adapt", "Adjust the plan", 2, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_10", EventStage.DISTRIBUTION, "Weather disruption", listOf(EventChoice("invest", "Allocate additional resources", 6, 2, 0), EventChoice("adapt", "Adjust the plan", 3, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_11", EventStage.DISTRIBUTION, "Distributor payment delay", listOf(EventChoice("invest", "Allocate additional resources", 7, 3, 1), EventChoice("adapt", "Adjust the plan", 4, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_12", EventStage.DISTRIBUTION, "Unexpected audience turnout", listOf(EventChoice("invest", "Allocate additional resources", 8, 4, 2), EventChoice("adapt", "Adjust the plan", 5, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_13", EventStage.DISTRIBUTION, "Extended theatrical run", listOf(EventChoice("invest", "Allocate additional resources", 9, 1, 0), EventChoice("adapt", "Adjust the plan", 2, 0, 1), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_14", EventStage.DISTRIBUTION, "Accessibility screening", listOf(EventChoice("invest", "Allocate additional resources", 10, 2, 1), EventChoice("adapt", "Adjust the plan", 3, 0, 2), EventChoice("continue", "Keep current plan", 0, -2, 0))),
        ProductionEvent("dist_15", EventStage.DISTRIBUTION, "International festival invitation", listOf(EventChoice("invest", "Allocate additional resources", 11, 3, 2), EventChoice("adapt", "Adjust the plan", 4, 0, 3), EventChoice("continue", "Keep current plan", 0, -2, 0)))
    )
}
