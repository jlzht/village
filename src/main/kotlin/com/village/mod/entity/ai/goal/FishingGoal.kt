package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Fisherman
import com.village.mod.world.Pond
import com.village.mod.world.StructureType
import com.village.mod.world.event.VillagerRequestCallback
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class FishingGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var lastUpdateTime: Long = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        val currentTime = entity.world.time
        if (currentTime - lastUpdateTime < 200L) {
            return false
        }
        if ((entity.getProfession() as Fisherman).getFishHook() != null) {
            LOGGER.info("CANCELLED HERE!")
            return false
        }
        lastUpdateTime = currentTime
        if (entity.workStructure.second == null) {
            VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.POND)
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
