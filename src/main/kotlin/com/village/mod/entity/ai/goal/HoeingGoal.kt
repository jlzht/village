package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.Hand
import java.util.EnumSet
import com.village.mod.world.event.VillagerRequestCallback

class HoeingGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var lastUpdateTime: Long = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        //if (entity.isState(State.WORK) && !entity.isTargetBlockEmpty()) {
        //    return entity.squaredDistanceTo(entity.peekTargetBlock().toCenterPos()) < 1.8
        //}
        val currentTime = entity.world.time
        if (currentTime - lastUpdateTime < 300L) {
            return false
        }
        lastUpdateTime = currentTime
        return true
    }

    override fun shouldContinue(): Boolean {
        return false
    }

    override fun stop() {
    }

    override fun start() {
      VillagerRequestCallback.EVENT.invoker().interact(entity, 2)
    }
}
