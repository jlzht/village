package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import com.village.mod.village.villager.State
import com.village.mod.village.villager.Task
import com.village.mod.world.event.VillagerRequestCallback
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class ActionGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var lastUpdateTime: Long = 0
    private val world = entity.world
    private var delay = 100L

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        val currentTime = entity.world.time
        if (currentTime - lastUpdateTime < delay) return false
        lastUpdateTime = currentTime
        return entity.errand.isEmpty() && entity.task.get() != Task.ALERT
    }

    override fun shouldContinue() = false

    override fun stop() {}
    override fun start() {
        if (entity.world.isClient) return
        this.handleStructureErrandsAssign()
    }

    private fun handleStructureErrandsAssign() {
        val shouldSleep = entity.shouldSleep((world.getTimeOfDay() / 1000) % 24.0f)
        if (!entity.ishomeok()) {
            VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.HOUSE)
        } else if (shouldSleep && !entity.state.isAt(State.SLEEP)) {
            entity.homeStructure.structure?.getErrands(world)?.let { errands ->
                entity.homeStructure.structure?.owners?.indexOf(entity.key)?.let { index ->
                    LOGGER.info("INDEX:{}", index)
                    if (!errands.isEmpty()) {
                        entity.errand.push(errands.get(index))
                    }
                    LOGGER.info("CANNOT LOCATE")
                }
            }
        }
        if (!shouldSleep) {
            entity.state.set(State.IDLE)
        }
        if (!entity.isworkok()) {
            VillagerRequestCallback.EVENT.invoker().interact(entity, entity.getProfession()!!.structureInterest)
        } else {
            entity.workStructure.structure?.getErrands(world)?.let { errands ->
                if (!errands.isEmpty()) { entity.errand.push(errands) }
            }
        }
    }
}
