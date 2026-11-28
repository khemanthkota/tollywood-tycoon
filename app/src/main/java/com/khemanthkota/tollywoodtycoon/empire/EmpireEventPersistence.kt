package com.khemanthkota.tollywoodtycoon.empire

import org.json.JSONArray
import org.json.JSONObject

internal object EmpireEventPersistence {
    fun encode(ledger: EventLedger): JSONObject = JSONObject()
        .put("pending", ledger.pendingId ?: "")
        .put("decisions", JSONArray().apply {
            ledger.decisions.forEach { d ->
                put(JSONObject().put("event", d.eventId).put("choice", d.choiceId)
                    .put("day", d.day).put("cost", d.cost).put("quality", d.quality).put("delay", d.delay))
            }
        })
    fun decode(json: JSONObject?): EventLedger {
        if (json == null) return EventLedger()
        val items = json.optJSONArray("decisions") ?: JSONArray()
        val decisions = (0 until items.length()).map { i ->
            val d = items.getJSONObject(i)
            EventDecision(d.getString("event"), d.getString("choice"), d.getInt("day"),
                d.getInt("cost"), d.getInt("quality"), d.getInt("delay"))
        }
        val pending = json.optString("pending").takeIf { it.isNotEmpty() && decisions.none { d -> d.eventId == it } }
        return EventLedger(pending, decisions)
    }
}
