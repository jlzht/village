package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.world.Farm
import com.village.mod.world.StructureType
import com.village.mod.world.event.VillagerRequestCallback
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class TillingGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var lastUpdateTime: Long = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        val currentTime = entity.world.time
        if (currentTime - lastUpdateTime < 400L) {
            return false
        }
        lastUpdateTime = currentTime
        if (entity.workStructure.second == null) {
            VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.FARM)
            return false
        }
        return true
    }

    override fun shouldContinue(): Boolean {
        return false
    }

    override fun stop() {
    }

    override fun start() {
      entity.workStructure.second!!.interaction(entity)
    }
}
