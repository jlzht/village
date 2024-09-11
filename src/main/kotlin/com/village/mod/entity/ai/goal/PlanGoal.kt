package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import com.village.mod.world.SettlementAccessor
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
        val manager = entity.getErrandsManager()
        if (manager.free == 0) {
            SettlementAccessor.visitSettlement(entity)
            SettlementAccessor.findSettlementToAttach(entity)
            // how to test if villager is interested in settling down?
        } else if (!manager.hasSettlement()) {
            SettlementAccessor.getSettlementToAttach(entity)
        } else {
            if (manager.home == 0) {
                LOGGER.info("WILL LOOK FOR HOUSE")
                SettlementAccessor.findStructureToAttach(entity, StructureType.HOUSE)
            } else if (!manager.hasHome()) {
                LOGGER.info("WILL ATTACH TO HOME - {}", manager.home)
                SettlementAccessor.getStructureToAttach(entity, manager.home)
            }
            if (manager.work == 0) {
                entity.getProfession()?.let { profession ->
                    SettlementAccessor.findStructureToAttach(entity, profession.structureInterest)
                }
            } else if (!manager.hasWork()) {
                SettlementAccessor.getStructureToAttach(entity, manager.work)
            }
            manager.update()
        }
    }
}
