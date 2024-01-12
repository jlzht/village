package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.VigerEntity

import net.minecraft.entity.EntityPose
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import java.util.EnumSet

class SitOnSurfaceGoal(private val entity: VigerEntity) : Goal() {
    private val itemEntityPredicate = EntityPredicates.VALID_ENTITY
    private var sitPos: BlockPos? = null

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.isRiding() || entity.isSitting()) {
            return false
        }
        sitPos = entity.getBlockPos()
        return entity.world.getBlockState(sitPos).isIn(BlockTags.SLABS)
    }

    override fun start() {
        var destPos: BlockPos = sitPos!!
        entity.getNavigation().startMovingTo(destPos.x.toDouble(), destPos.y - 0.5, destPos.z.toDouble(), 1.0)
        entity.setPose(EntityPose.SITTING)
    }

    override fun shouldContinue(): Boolean {
        return sitPos!!.getSquaredDistance(entity.getPos()) <= 1.20
    }

    override fun stop() {
        entity.setPose(EntityPose.STANDING)
        sitPos = null
    }
}
