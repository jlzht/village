package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import net.minecraft.block.BedBlock
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import java.util.EnumSet

class SleepGoal(private val entity: CustomVillagerEntity) : Goal() {
    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.isState(State.IDLE)) {
            val blockState = entity.getWorld().getBlockState(entity.getBlockPos())
            return this.getSleepTime() && blockState.isIn(BlockTags.BEDS) && !blockState.get(BedBlock.OCCUPIED)
        }
        return false
    }

    override fun start() {
        LOGGER.info("I WILL START TO SLEEP!")
        if (entity.isState(State.IDLE)) {
            entity.setState(State.SLEEP)
            entity.lockState()
            entity.sleep(entity.getBlockPos())
            LOGGER.info("I STARTED START TO SLEEP!")
        }
    }

    override fun shouldContinue(): Boolean {
        return this.getSleepTime()
    }

    override fun stop() {
        entity.wakeUp()
        entity.unlockState()
        entity.setState(State.IDLE)
    }
    private fun getSleepTime(): Boolean {
        val serverWorld = entity.world as ServerWorld
        val timeOfDay = serverWorld.timeOfDay
        val hour = ((timeOfDay / 1000)).toInt()
        if (hour > 16 || hour < 2) {
            return true
        }
        return false
    }
}
