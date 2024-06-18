package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.village.profession.Guard
import com.village.mod.village.villager.Action
import net.minecraft.entity.LivingEntity
//import net.minecraft.entity.ai.NoPenaltyTargeting
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.math.BlockPos
import java.util.EnumSet

class ReactGoal(private val entity: CustomVillagerEntity, private val speed: Double) : Goal() {
    private var seeingTargetTicker: Int = 0
    private var unchargedTicksLeft: Int = 10
    private var squaredRange: Float = 0.0f

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        return this.hasAliveTarget()
    }

    override fun shouldContinue(): Boolean {
        return this.hasAliveTarget()
    }

    private fun hasAliveTarget(): Boolean {
        return entity.target != null && entity.getTarget()!!.isAlive()
    }

    override fun stop() {
        entity.navigation.stop()
        this.seeingTargetTicker = 0
        entity.setAttacking(false)
        if (entity.isUsingItem()) {
            entity.clearActiveItem()
            entity.setCharging(false)
        }
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    // TODO: reimplement: Fleeing and talking
    private fun targetHandler(livingEntity: LivingEntity, shouldMoveTowardsTarget: Boolean) {
        if (shouldMoveTowardsTarget) {
            entity.navigation.startMovingTo(livingEntity, 1.0 * if (entity.isUsingItem) { if (entity.isHoldingSword()) { 1.25f } else { 0.5f } } else { 1.15f })
        } else {
            entity.navigation.stop()
        }
        entity.lookControl.lookAt(livingEntity, 30.0f, 30.0f)
        entity.yaw = entity.headYaw
    }

    override fun tick() {
        val livingEntity = entity.target ?: return
        val canSeeTarget = entity.visibilityCache.canSee(livingEntity)
        if (canSeeTarget != seeingTargetTicker > 0) { seeingTargetTicker = 0 }
        seeingTargetTicker = if (canSeeTarget) { seeingTargetTicker + 1 } else { seeingTargetTicker - 1 }
        val distanceSquared = entity.squaredDistanceTo(livingEntity)
        val shouldMoveTowardsTarget = (distanceSquared > squaredRange || seeingTargetTicker < 5)
        if (entity.isAttacking()) {
            // Attacking/Fleeing mobs
            if (entity.canAttack() && (entity.getProfession() as Guard).canWork()) {
                this.targetHandler(livingEntity, shouldMoveTowardsTarget)
                (entity.getProfession() as Guard).let {
                    it.doWork()
                    squaredRange = it.range
                }
            } else {
                // get target angle, get most distant angle, if cannot find try create, if cant create get any node
                //this.fleeHandler(livingEntity)
            }
        } else {
            // Talking with villagers
            this.targetHandler(livingEntity, distanceSquared > 3.0 + entity.random.nextInt(4).toDouble())
            if (unchargedTicksLeft <= -10) {
                unchargedTicksLeft = 0
                if (entity.random.nextInt(10) != 0) return
                //this.fleeHandler(livingEntity)
                entity.setTarget(null)
                return
            }
            unchargedTicksLeft--
        }
    }
}
