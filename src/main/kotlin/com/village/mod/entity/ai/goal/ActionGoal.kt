package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import com.village.mod.village.villager.State
import com.village.mod.world.event.VillagerRequestCallback
import net.minecraft.util.Hand
import net.minecraft.item.ItemStack
import net.minecraft.entity.ai.goal.Goal
import java.util.EnumSet

class ActionGoal(private val entity: CustomVillagerEntity) : Goal() {
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
        this.handleStructureErrandsAssign()
    }

    private fun handleStructureErrandsAssign() {
        val shouldSleep = entity.shouldSleep((world.getTimeOfDay() / 1000) % 24.0f)
        if (!entity.ishomeok()) {
            VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.HOUSE)
        } else if (shouldSleep && !entity.state.isAt(State.SLEEP)) {
            entity.homeStructure.structure?.getErrands(world)?.let { errands ->
                entity.homeStructure.structure?.owners?.indexOf(entity.key)?.let { index ->
                    if (!errands.isEmpty()) {
                        entity.errand.push(errands.get(index))
                    }
                }
            }
        }
        if (!shouldSleep) entity.wakeUp()
        if (entity.getProfession() != null && !entity.isActing()) {
            if (!entity.isworkok()) {
                VillagerRequestCallback.EVENT.invoker().interact(entity, entity.getProfession()!!.structureInterest)
            } else if (entity.getProfession()!!.canWork(entity)) {
                entity.workStructure.structure?.getErrands(world)?.let { errands ->
                    if (!errands.isEmpty()) entity.errand.push(errands)
                }
            }
        }
    }
}
