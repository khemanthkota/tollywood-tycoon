package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class StudioAccountingTest {
    @Test fun benefitsIncreasePayroll() {
        assertEquals(118.2,StudioAccounting.payroll(100.0,10,2.0,AccountingPolicy()).total,0.0001)
    }
    @Test fun lossesOffsetTaxableProfit() {
        val result = AnnualAccounts(100.0,40.0,10.0,5.0,5.0,5.0,5.0,20.0).close(AccountingPolicy())
        assertEquals(2.5,result.tax,0.0001)
        assertEquals(27.5,result.netProfit,0.0001)
    }
    @Test fun lossCarriesForward() {
        val result = AnnualAccounts(10.0,30.0,0.0,0.0,0.0,0.0,0.0,5.0).close(AccountingPolicy())
        assertEquals(0.0,result.tax,0.0001)
        assertEquals(25.0,result.carriedLoss,0.0001)
    }
    @Test fun excessIndirectTaxCreditCarriesForward() {
        assertEquals(0.0 to 5.0,StudioAccounting.indirectTax(10.0,15.0))
    }
}
