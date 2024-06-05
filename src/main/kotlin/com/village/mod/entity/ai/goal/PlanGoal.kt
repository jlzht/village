package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import com.village.mod.world.event.VillagerRequestCallback
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class PlanGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        if (entity.random.nextInt(10) != 0) {
            return false
        }
        return entity.errand.isEmpty()
    }

    override fun shouldContinue() = false

    override fun start() {
        if (entity.world.isClient) return
    }
}
