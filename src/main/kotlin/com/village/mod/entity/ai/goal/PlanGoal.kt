package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import com.village.mod.world.SettlementManager
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class PlanGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world
    // holds a buffer of previous errands

    init {
        setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        if (entity.random.nextInt(20) != 0) return false
        return !entity.getErrandsManager().hasErrands()
    }

    override fun shouldContinue() = false

    override fun start() {
        if (entity.world.isClient) return
        if (!entity.hasVillage()) {
            SettlementManager.visitSettlement(entity)
            SettlementManager.joinSettlement(entity)
            // how to test if villager is interested in settling down?
        } else {
            if (!entity.getErrandsManager().hasHome()) {
                SettlementManager.assignStructure(entity, StructureType.HOUSE)
            }
            if (!entity.getErrandsManager().hasWork()) {
                entity.getProfession()?.let { profession ->
                    SettlementManager.assignStructure(entity, profession.structureInterest)
                }
            }
            entity.getErrandsManager().update()
        }
    }
}
