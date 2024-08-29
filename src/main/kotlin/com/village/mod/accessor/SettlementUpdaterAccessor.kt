package com.village.mod.accessor

import com.village.mod.world.SettlementManager

public interface SettlementManagerAccessor {
    fun setSettlementsUpdateInterval(ticks: Long)
    fun getSettlementManager(): SettlementManager
}
