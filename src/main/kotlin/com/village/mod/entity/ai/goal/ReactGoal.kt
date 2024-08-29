package com.village.mod.entity.ai.goal

import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.ai.NoPenaltyTargeting
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.math.BlockPos
import java.util.EnumSet

class ReactGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var seeingTargetTicker: Int = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        return entity.target != null && entity.getTarget()!!.isAlive()
    }

    override fun shouldContinue(): Boolean = false

    override fun shouldRunEveryTick(): Boolean = true

    override fun tick() {
        val livingEntity = entity.target ?: return
        val canSeeTarget = entity.visibilityCache.canSee(livingEntity)
        if (canSeeTarget != seeingTargetTicker > 0) { seeingTargetTicker = 0 }
        seeingTargetTicker = if (canSeeTarget) { seeingTargetTicker + 1 } else { seeingTargetTicker - 1 }
        val distanceSquared = entity.squaredDistanceTo(livingEntity)
        val errand = entity.getErrandsManager().peek()

        // needs more testing
        if (errand == null || (errand.cid != Action.Type.SHOOT && errand.cid != Action.Type.FLEE && errand.cid != Action.Type.ATTACK)) {
            if (distanceSquared <= 12) {
                if (!entity.getErrandsManager().add(Action.Type.ATTACK, BlockPos(0, 0, 0))) {
                    NoPenaltyTargeting.findFrom(entity, 8, 3, livingEntity.getPos())?.let { t ->
                        entity.getErrandsManager().add(Action.Type.FLEE, BlockPos(t.x.toInt(), t.y.toInt(), t.z.toInt()))
                    }
                }
            } else {
                if (!entity.getErrandsManager().add(Action.Type.SHOOT, livingEntity.blockPos.up())) {
                    entity.getErrandsManager().add(Action.Type.ATTACK, BlockPos(0, 0, 0))
                }
            }
        }
    }
}
