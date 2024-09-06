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
            val manager = entity.getErrandsManager()
            // if (manager.homeID == 0) {
            //    LOGGER.info("WILL LOOK FOR HOUSE")
            //    SettlementManager.assignStructure(entity, StructureType.HOUSE)
            // } else if (!manager.hasHome()) {
            //    LOGGER.info("WILL ATTACH TO HOME - {}", manager.homeID)
            //    SettlementManager.attachStructure(entity, manager.homeID)
            // }
            if (manager.workID == 0) {
                entity.getProfession()?.let { profession ->
                    SettlementManager.assignStructure(entity, profession.structureInterest)
                }
            } else if (!manager.hasWork()) {
                SettlementManager.attachStructure(entity, manager.workID)
            }
            manager.update()
        }
    }
}
