package com.khemanthkota.tollywoodtycoon.empire

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionBudgetTest {
    @Test fun exactBudgetIsAffordable() {
        assertEquals(0L, productionFundsAvailable(300, 200, 40, 30, 30))
    }
    @Test fun payrollCannotBeSpentOnCraft() {
        assertEquals(-10L, productionFundsAvailable(300, 200, 40, 40, 30))
    }
    @Test fun equipmentResponseMustRespectPayroll() {
        assertEquals(-20L, productionFundsAvailable(300, 200, 40, 30, 50))
    }
    @Test fun greenlightReservesPayrollAndContingency() {
        assertEquals(-1L, productionFundsAvailable(300, 231, 40, contingency = 30))
    }
    @Test fun largeCostsDoNotOverflow() {
        assertEquals(-4294967294L, productionFundsAvailable(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE))
    }
}
