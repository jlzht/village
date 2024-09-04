package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.world.SettlementManager
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class PlanGoal(
    private val entity: CustomVillagerEntity,
) : Goal() {
    private val world = entity.world

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.random.nextInt(10) != 0) return false
        return !entity.getErrandsManager().hasErrands()
    }

    override fun shouldContinue() = false

    override fun start() {
        LOGGER.info("Entering")
        if (entity.world.isClient) return
        if (!entity.hasVillage()) {
            SettlementManager.visitSettlement(entity)
            SettlementManager.joinSettlement(entity)
            // how to test if villager is interested in settling down?
        } else {
            // if (!entity.getErrandsManager().hasHome()) {
            //    SettlementManager.assignStructure(entity, StructureType.HOUSE)
            // }
            if (!entity.getErrandsManager().hasWork()) {
                entity.getProfession()?.let { profession ->
                    SettlementManager.assignStructure(entity, profession.structureInterest)
                }
            }
        }

        if (entity.getErrandsManager().hasWork()) {
            entity.getErrandsManager().update()
        }
    }
}
