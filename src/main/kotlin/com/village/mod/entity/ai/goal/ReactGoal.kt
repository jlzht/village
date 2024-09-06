package com.village.mod.entity.ai.goal

import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.util.Finder
import com.village.mod.village.profession.Guard
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.registry.tag.FluidTags
import java.util.EnumSet

class ReactGoal(
    private val entity: CustomVillagerEntity,
) : Goal() {
    private var seeingTargetTicker: Int = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean = true

    override fun shouldContinue(): Boolean = false

    override fun shouldRunEveryTick(): Boolean = true

    override fun tick() {
        if (entity.isTouchingWater() && entity.getFluidHeight(FluidTags.WATER) > entity.getSwimHeight()) {
            val g = if (entity.horizontalCollision) 0.3f else 0.15f
            if (entity.getRandom().nextFloat() < g || entity.getAir() < 20) {
                entity.getJumpControl().setActive()
            }
        }

        entity.target?.let { target ->
            if (!target.isAlive) {
                entity.target = null
                return
            }
            val errand = entity.getErrandsManager().peek()
            if (
                errand == null ||
                (
                    errand.cid != Action.Type.ATTACK &&
                        errand.cid != Action.Type.CHARGE &&
                        errand.cid != Action.Type.AIM &&
                        errand.cid != Action.Type.FLEE &&
                        errand.cid != Action.Type.LOOK &&
                        errand.cid != Action.Type.DEFEND
                )
            ) {
                if (entity.isFighting()) {
                    if (entity.getProfession() is Guard && entity.isAttacking()) {
                        val distanceSquared = entity.squaredDistanceTo(target)
                        if (!(
                                distanceSquared >= 12 &&
                                    (
                                        entity.getErrandsManager().add(
                                            Action.Type.AIM,
                                        ) ||
                                            entity.getErrandsManager().add(Action.Type.CHARGE)
                                    )
                            )
                        ) {
                            val attack = entity.getErrandsManager().add(Action.Type.ATTACK)
                            var defend: Boolean = false
                            if (distanceSquared <= 4) {
                                defend = entity.getErrandsManager().add(Action.Type.DEFEND)
                            }
                            if (!attack || entity.random.nextInt() == 0) {
                                if (!defend || entity.random.nextInt() == 0) {
                                    entity.setAttacking(false)
                                    if (!entity.getErrandsManager().has(Action.Type.FLEE)) {
                                        Finder.findFleeBlock(entity, target)?.let { block ->
                                            entity.getErrandsManager().add(
                                                block,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TODO: create Retreat action (find new position based on target looking angle)
                        if (!entity.getErrandsManager().has(Action.Type.FLEE)) {
                            Finder.findFleeBlock(entity, target)?.let { block ->
                                entity.getErrandsManager().add(
                                    block,
                                )
                            }
                        }
                    }
                } else {
                    // tweak this
                    if (entity.random.nextInt(10) == 0) {
                        if (!entity.getErrandsManager().has(Action.Type.LOOK)) {
                            entity.getErrandsManager().add(Action.Type.LOOK)
                        }
                        return
                    } else {
                        // Fail look logic if needed
                    }
                }
            }
        }
    }
}
