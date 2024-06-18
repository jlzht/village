package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.ai.goal.Goal
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.village.structure.StructureType
import net.minecraft.util.math.MathHelper
import java.util.EnumSet

class PlanGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world
    val traitA = 0.5f
    val traitB = 0.5f

    init {
        setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        // FIXME
        if (entity.random.nextInt(10) != 0) {
            return false
        }
        // try start on node reach
        return entity.errand.isEmpty()
    }

    override fun shouldContinue() = false

    override fun start() {
        if (entity.world.isClient) return
        VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.NONE )
        // updateGroupAction(0)
        // if no home find | if no work find | else*else traverse
        // VillagerRequestCallback.EVENT.invoker().interact(entity, intArrayOf(2,2) )
    }

    private fun updateGroupAction(time: Float) {
        val a = MathHelper.sin(MathHelper.PI * (time + 2 * (1 - traitA)) / (2 * (6 - traitB)))
        val b = MathHelper.sin(MathHelper.PI * ((time - 2) - (1 - traitA)) / (2 * (3 + traitB)))
        val g = MathHelper.floor(1 - a) * MathHelper.floor(2 - b)
        LOGGER.info("Group: {}", g)
        when (g) {
            0 -> { // SLEEP
                if (entity.intr.hasHome()) {
                    if (!entity.isSleeping()) {
                        // VillagerRequestCallback.EVENT.invoker().interact(entity, intArrayOf(2,2) ) request errands to sleep
                    }
                } else {
                    VillagerRequestCallback.EVENT.invoker().interact(entity, StructureType.HOUSE)
                }
            }
            1 -> { // WORK
                if (entity.intr.hasWork()) {
                    if (entity.canWork()) {
                        // VillagerRequestCallback.EVENT.invoker().interact(entity, intArrayOf(2,2) ) request errands to sleep
                    }
                } else {
                    VillagerRequestCallback.EVENT.invoker().interact(entity, entity.getStructureOfInterest())
                }
            }
            2 -> { // IDLE
                if (entity.isSleeping()) entity.wakeUp()
            }
        }
    }
}
