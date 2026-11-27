package com.khemanthkota.tollywoodtycoon.empire

// Monetary values are in lakh rupees, consistent with the career engine.
data class AssetTechnology(
    val id: String, val name: String, val earliestYear: Int,
    val price: Int, val supply: Int, val requiredFilms: Int,
    val requiredAchievements: Set<String>, val prerequisites: Set<String> = emptySet(),
    val maintenancePercent: Int = 3, val annualDepreciationPercent: Int = 8
) {
    init { require(price > 0 && supply > 0 && requiredFilms >= 0) }
}
data class AssetEligibility(val films: Int, val achievements: Set<String>)
data class IndustryTechnology(val year: Int, val discovered: Set<String>)
data class StudioAsset(val id: String, val technologyId: String, val owner: String?,
    val value: Int, val debt: Int = 0, val renter: String? = null,
    val bookedUntil: Int = 0, val auction: Boolean = false)
data class AssetEconomy(val assets: List<StudioAsset>, val balances: Map<String, Int>) {
    private fun balance(studio: String) = balances[studio] ?: 0
    private fun replace(asset: StudioAsset, money: Map<String, Int> = balances) =
        copy(assets = assets.map { if (it.id == asset.id) asset else it }, balances = money)
    fun buy(id: String, buyer: String, price: Int, eligibility: AssetEligibility,
            industry: IndustryTechnology): AssetEconomy {
        val a = assets.single { it.id == id }
        val t = EmpireAssetCatalog.allTechnologies.single { it.id == a.technologyId }
        require(a.owner != buyer && a.debt == 0 && a.renter == null && !a.auction)
        require(price > 0 && balance(buyer) >= price && eligible(t, eligibility, industry))
        val money = balances.toMutableMap()
        money[buyer] = balance(buyer) - price
        a.owner?.let { money[it] = balance(it) + price }
        return replace(a.copy(owner = buyer, value = price), money)
    }
    fun rent(id: String, renter: String, day: Int, days: Int, fee: Int): AssetEconomy {
        val a = assets.single { it.id == id }
        val owner = requireNotNull(a.owner)
        require(owner != renter && !a.auction && a.bookedUntil <= day && days > 0 && fee > 0)
        require(balance(renter) >= fee)
        return replace(a.copy(renter = renter, bookedUntil = day + days),
            balances + (renter to balance(renter)-fee) + (owner to balance(owner)+fee))
    }
    // Add newly discovered inventory once, preserving all existing ownership and debt.
    fun refreshSupply(industry: IndustryTechnology): AssetEconomy {
        val existing = assets.map { it.id }.toSet()
        return copy(assets = assets + EmpireAssetCatalog.initialAssets(industry).filter { it.id !in existing })
    }
    // Annual maintenance is an explicit cash obligation; unpaid bills are returned
    // to the caller for financial resolution rather than silently creating money.
    data class AnnualCosts(val economy: AssetEconomy, val unpaid: Map<String, Int>)
    fun annualUpdate(): AnnualCosts {
        val money = balances.toMutableMap()
        val unpaid = mutableMapOf<String, Int>()
        val catalog = EmpireAssetCatalog.allTechnologies.associateBy { it.id }
        val updated = assets.map { a ->
            val t = catalog.getValue(a.technologyId)
            val owner = a.owner
            if (owner != null) {
                val bill = (a.value * t.maintenancePercent / 100).coerceAtLeast(1)
                val paid = minOf(bill, (money[owner] ?: 0).coerceAtLeast(0))
                money[owner] = (money[owner] ?: 0) - paid
                if (paid < bill) unpaid[owner] = (unpaid[owner] ?: 0) + bill - paid
                a.copy(value = (a.value * (100-t.annualDepreciationPercent)/100).coerceAtLeast(1))
            } else a
        }
        return AnnualCosts(copy(assets = updated, balances = money), unpaid)
    }
    fun releaseBookings(day: Int) = copy(assets = assets.map {
        if (it.bookedUntil <= day) it.copy(renter = null) else it
    })
    fun mortgage(id: String, owner: String, principal: Int): AssetEconomy {
        val a = assets.single { it.id == id }
        require(a.owner == owner && a.debt == 0 && !a.auction && principal in 1..(a.value / 2))
        return replace(a.copy(debt = principal), balances + (owner to balance(owner)+principal))
    }
    fun repay(id: String, owner: String, amount: Int): AssetEconomy {
        val a = assets.single { it.id == id }
        require(a.owner == owner && !a.auction && amount in 1..a.debt && balance(owner) >= amount)
        return replace(a.copy(debt = a.debt-amount), balances + (owner to balance(owner)-amount))
    }
    // Called by a future loan scheduler only after contractual default and grace period.
    fun repossess(id: String): AssetEconomy {
        val a = assets.single { it.id == id }
        require(a.debt > 0 && !a.auction && a.renter == null)
        return replace(a.copy(auction = true))
    }
    // Settlement of a winning bid; bidding and residual debt accounting are separate future systems.
    fun settleAuction(id: String, buyer: String, bid: Int, eligibility: AssetEligibility,
                      industry: IndustryTechnology): AssetEconomy {
        val a = assets.single { it.id == id }
        val t = EmpireAssetCatalog.allTechnologies.single { it.id == a.technologyId }
        require(a.auction && buyer != a.owner && bid >= a.debt && balance(buyer) >= bid)
        require(eligible(t, eligibility, industry))
        val money = balances.toMutableMap()
        money[buyer] = balance(buyer)-bid
        a.owner?.let { money[it] = balance(it)+(bid-a.debt) }
        return replace(a.copy(owner = buyer, debt = 0, auction = false, value = bid), money)
    }
    companion object {
        fun eligible(t: AssetTechnology, e: AssetEligibility, i: IndustryTechnology) =
            i.year >= t.earliestYear && t.id in i.discovered &&
            i.discovered.containsAll(t.prerequisites) && e.films >= t.requiredFilms &&
            e.achievements.containsAll(t.requiredAchievements)
    }
}
object EmpireAssetCatalog {
    // Illustrative technology gates, not forecasts. Discovery is separate from calendar availability.
    val technologies = listOf(
        AssetTechnology("digital4k","Digital cinema camera",2026,40,30,3,emptySet()),
        AssetTechnology("largeformat","Large-format cinema camera",2026,250,4,8,setOf("cinematography")),
        AssetTechnology("ledstage","LED virtual production stage",2026,600,3,10,setOf("technical"),setOf("digital4k")),
        AssetTechnology("robotcamera","Autonomous robotic camera rig",2035,180,8,12,setOf("technical"),setOf("digital4k")),
        AssetTechnology("volumetric","Volumetric capture stage",2045,1200,4,15,setOf("technical"),setOf("ledstage")),
        AssetTechnology("lightfield","Light-field cinema display",2060,1800,3,20,setOf("distribution"),setOf("volumetric")),
        AssetTechnology("haptic","Multisensory theatre",2080,2500,2,25,setOf("distribution"),setOf("lightfield")),
        AssetTechnology("orbital","Orbital filming facility access",2100,5000,2,30,setOf("technical","international"),setOf("robotcamera"))
    )
    val allTechnologies: List<AssetTechnology> by lazy { technologies + EmpireTechnologyRoadmap.variants.map { it.technology } }
    fun initialAssets(industry: IndustryTechnology) = allTechnologies.filter {
        industry.year >= it.earliestYear && it.id in industry.discovered &&
            industry.discovered.containsAll(it.prerequisites)
    }.flatMap { t -> (1..t.supply).map { n -> StudioAsset("${t.id}-$n",t.id,null,t.price) } }
}
