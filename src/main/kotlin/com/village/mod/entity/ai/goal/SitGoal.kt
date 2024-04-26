package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import com.village.mod.LOGGER
import net.minecraft.entity.EntityPose
import java.util.EnumSet

class SitGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var sitPos: BlockPos? = null
    //private var prevState: State = State.NONE

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.isState(State.SIT)) {
            //sitPos = entity.getBlockPos()
            //if (entity.world.getBlockState(sitPos).isIn(BlockTags.SLABS)) {
                //prevState = entity.getState()
                //entity.setState(State.SIT)
                //entity.lockState()
                //LOGGER.info("I WILL SIT!")
                return true
            //}
        }
        return false
    }

    override fun start() {
        LOGGER.info("I START TO SIT!")
        val destPos: Vec3d = sitPos!!.toCenterPos()
        entity.setPosition(destPos.getX(), destPos.getY(), destPos.getZ());
        entity.setPose(EntityPose.SITTING)
    }

    override fun shouldContinue(): Boolean {
        return !entity.isState(State.SIT) || entity.world.getBlockState(entity.getBlockPos()).isIn(BlockTags.SLABS)
    }

    override fun stop() {
        LOGGER.info("I WILL STOP TO SIT")
        entity.setPose(EntityPose.STANDING)
    }
}
