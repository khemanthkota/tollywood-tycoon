package com.khemanthkota.tollywoodtycoon.empire

data class AssetBidder(val studio: String, val eligibility: AssetEligibility,
    val reserveCash: Int, val preferredCategories: Set<String>)
data class AssetBid(val studio: String, val amount: Int)
data class AssetAuctionResult(val economy: AssetEconomy, val winner: AssetBid?, val bids: List<AssetBid>)

object EmpireAssetCompetition {
    fun rivalBid(economy: AssetEconomy, asset: StudioAsset, bidder: AssetBidder,
                 industry: IndustryTechnology): AssetBid? {
        val t = EmpireAssetCatalog.allTechnologies.single { it.id == asset.technologyId }
        if (asset.owner == bidder.studio || !AssetEconomy.eligible(t,bidder.eligibility,industry)) return null
        val category = EmpireTechnologyRoadmap.variants.firstOrNull { it.technology.id == t.id }?.category
        val owned = economy.assets.count { it.owner == bidder.studio && it.technologyId == t.id }
        val multiplier = if (category in bidder.preferredCategories) 115 else 85
        val valuation = asset.value * multiplier / 100 / (owned+1)
        val available = (economy.balances[bidder.studio] ?: 0) - bidder.reserveCash
        val bid = minOf(valuation, available)
        return if (bid >= maxOf(1,asset.debt)) AssetBid(bidder.studio,bid) else null
    }
    // Sealed bids: all participants use the same eligibility and cash constraints.
    fun settle(economy: AssetEconomy, id: String, bidders: List<AssetBidder>,
               bids: List<AssetBid>, industry: IndustryTechnology, reserve: Int): AssetAuctionResult {
        val asset = economy.assets.single { it.id == id }
        val technology = EmpireAssetCatalog.allTechnologies.single { it.id == asset.technologyId }
        require(reserve > 0 && (asset.auction || asset.debt == 0))
        val valid = bids.filter { bid ->
            val bidder = bidders.firstOrNull { it.studio == bid.studio }
            bidder != null && bid.studio != asset.owner &&
                bid.amount >= maxOf(reserve,asset.debt) &&
                bid.amount <= (economy.balances[bid.studio] ?: 0) &&
                AssetEconomy.eligible(technology,bidder.eligibility,industry)
        }.sortedWith(compareByDescending<AssetBid> { it.amount }.thenBy { it.studio })
        val winner = valid.firstOrNull() ?: return AssetAuctionResult(economy,null,valid)
        val eligibility = bidders.first { it.studio == winner.studio }.eligibility
        val updated = if (asset.auction) economy.settleAuction(id,winner.studio,winner.amount,eligibility,industry)
            else economy.buy(id,winner.studio,winner.amount,eligibility,industry)
        return AssetAuctionResult(updated,winner,valid)
    }
}

// Principal-only prototype: interest and restructuring will be added to the financing engine.
data class CollateralSchedule(val assetId: String, val owner: String,
    val installment: Int, val nextDueDay: Int, val intervalDays: Int = 30,
    val graceDays: Int = 30) {
    init { require(installment > 0 && intervalDays > 0 && graceDays >= 0) }
}
data class CollateralTick(val economy: AssetEconomy, val schedule: CollateralSchedule?, val overdue: Boolean)
object EmpireCollateralScheduler {
    fun tick(economy: AssetEconomy, schedule: CollateralSchedule, day: Int): CollateralTick {
        var state = economy
        var due = schedule.nextDueDay
        while (day >= due) {
            val asset = state.assets.single { it.id == schedule.assetId }
            if (asset.owner != schedule.owner || asset.debt == 0 || asset.auction)
                return CollateralTick(state,null,false)
            val payment = minOf(schedule.installment,asset.debt)
            if ((state.balances[schedule.owner] ?: 0) >= payment) {
                state = state.repay(asset.id,schedule.owner,payment)
                due += schedule.intervalDays
                if (payment == asset.debt) return CollateralTick(state,null,false)
            } else {
                if (day >= due + schedule.graceDays && asset.renter == null)
                    return CollateralTick(state.repossess(asset.id),null,true)
                return CollateralTick(state,schedule.copy(nextDueDay=due),true)
            }
        }
        return CollateralTick(state,schedule.copy(nextDueDay=due),false)
    }
}
