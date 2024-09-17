package com.settlement.mod.entity.ai.goal

import com.settlement.mod.LOGGER
import com.settlement.mod.action.Action
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.util.Finder
import com.settlement.mod.profession.Combatant
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.registry.tag.FluidTags
import java.util.EnumSet

class ReactGoal(
    private val entity: AbstractVillagerEntity,
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
            val peek = entity.errandManager.peek()
            if (peek == null || peek.cid !in combatSet) {
                if (entity.isFighting()) {
                    if (entity.getProfession() is Combatant && entity.isAttacking()) {
                        val distance = entity.squaredDistanceTo(target)
                        LOGGER.info("-> {}", distance)
                        val cache = (entity.getProfession() as Combatant).cache
                        // Not quite right, make a combat errand provider per Profession
                        val canDamage =
                            entity.health < 5 &&
                                when {
                                    distance >= 16 -> {
                                        cache[Action.Type.CHARGE]?.takeIf { it }?.let { entity.pushErrand(Action.Type.CHARGE) }
                                            ?: cache[Action.Type.AIM]?.takeIf { it }?.let { entity.pushErrand(Action.Type.AIM) }
                                            ?: cache[Action.Type.ATTACK]?.takeIf { it }?.let { entity.pushErrand(Action.Type.ATTACK) }
                                            ?: false
                                    }
                                    distance <= 5 -> {
                                        if (entity.random.nextInt(3) == 0) { // 33% chance of defending
                                            cache[Action.Type.DEFEND]?.takeIf { it }?.let { entity.pushErrand(Action.Type.DEFEND) }
                                                ?: cache[Action.Type.ATTACK]?.takeIf { it }?.let { entity.pushErrand(Action.Type.ATTACK) }
                                                ?: false
                                        } else {
                                            cache[Action.Type.ATTACK]?.takeIf { it }?.let { entity.pushErrand(Action.Type.ATTACK) }
                                                ?: false
                                        }
                                    }
                                    else -> false
                                }
                        if (!canDamage) {
                            entity.setAttacking(false)
                            // TODO: create RETREAT and STRIFE actions
                            if (!entity.errandManager.has(Action.Type.FLEE)) {
                                Finder.findFleeBlock(entity, target)?.let { (cid, pos) ->
                                    entity.pushErrand(cid, pos)
                                }
                            }
                        }
                    } else {
                        entity.setAttacking(false)
                        if (!entity.errandManager.has(Action.Type.FLEE)) {
                            Finder.findFleeBlock(entity, target)?.let { (cid, pos) ->
                                entity.pushErrand(cid, pos)
                            }
                        }
                    }
                } else if (peek == null || !InteractionSet.contains(peek.cid)) {
                    if (entity.random.nextInt(20) == 0) {
                        if (!entity.errandManager.has(Action.Type.LOOK)) {
                            entity.pushErrand(Action.Type.LOOK)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val combatSet = setOf(Action.Type.ATTACK, Action.Type.CHARGE, Action.Type.AIM, Action.Type.FLEE, Action.Type.DEFEND)
        val InteractionSet = setOf(Action.Type.LOOK)
    }
}
