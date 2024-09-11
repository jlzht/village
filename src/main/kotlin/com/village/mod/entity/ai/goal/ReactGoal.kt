package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.util.Finder
import com.village.mod.village.profession.Combatant
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.registry.tag.FluidTags
import java.util.EnumSet

class ReactGoal(
    private val entity: CustomVillagerEntity,
) : Goal() {
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
            val peek = entity.getErrandsManager().peek()
            if (
                peek == null ||
                (
                    peek.cid != Action.Type.ATTACK &&
                        peek.cid != Action.Type.CHARGE &&
                        peek.cid != Action.Type.AIM &&
                        peek.cid != Action.Type.FLEE &&
                        peek.cid != Action.Type.LOOK &&
                        peek.cid != Action.Type.DEFEND
                )
            ) {
                if (entity.isFighting()) {
                    if (entity.getProfession() is Combatant && entity.isAttacking()) {
                        val distance = entity.squaredDistanceTo(target)
                        val errandsManager = entity.getErrandsManager()
                        LOGGER.info("-> {}", distance)
                        val cache = (entity.getProfession() as Combatant).cache
                        val canDamage =
                            when {
                                distance >= 16 -> {
                                    cache[Action.Type.CHARGE]?.takeIf { it }?.let { errandsManager.add(Action.Type.CHARGE) }
                                        ?: cache[Action.Type.AIM]?.takeIf { it }?.let { errandsManager.add(Action.Type.AIM) }
                                        ?: cache[Action.Type.ATTACK]?.takeIf { it }?.let { errandsManager.add(Action.Type.ATTACK) }
                                        ?: false
                                }
                                distance <= 5 -> {
                                    if (entity.random.nextInt(3) == 0) { // 33% chance of defending
                                        cache[Action.Type.DEFEND]?.takeIf { it }?.let { errandsManager.add(Action.Type.DEFEND) }
                                            ?: cache[Action.Type.ATTACK]?.takeIf { it }?.let { errandsManager.add(Action.Type.ATTACK) }
                                            ?: false
                                    } else {
                                        cache[Action.Type.ATTACK]?.takeIf { it }?.let { errandsManager.add(Action.Type.ATTACK) }
                                            ?: false
                                    }
                                }
                                else -> false
                            }
                        LOGGER.info("CANT DAMAGE!")
                        if (!canDamage) {
                            entity.setAttacking(false)
                            // TODO: create RETREAT and STRIFE actions
                            if (!errandsManager.has(Action.Type.FLEE)) {
                                Finder.findFleeBlock(entity, target)?.let { block ->
                                    errandsManager.add(block)
                                }
                            }
                            // low health
                        }
                    } else {
                        entity.setAttacking(false)
                        if (!entity.getErrandsManager().has(Action.Type.FLEE)) {
                            Finder.findFleeBlock(entity, target)?.let { block ->
                                entity.getErrandsManager().add(
                                    block,
                                )
                            }
                        }
                    }
                } else {
                    if (entity.random.nextInt(20) == 0) {
                        if (!entity.getErrandsManager().has(Action.Type.LOOK)) {
                            entity.getErrandsManager().add(Action.Type.LOOK)
                        }
                    }
                }
            }
        }
    }
}
