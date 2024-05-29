package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.village.profession.Guard
import com.village.mod.village.villager.Action
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.NoPenaltyTargeting
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.math.BlockPos
import java.util.EnumSet

class AttackGoal(private val entity: CustomVillagerEntity, private val speed: Double) : Goal() {
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
        entity.setTarget(null)
        if (entity.isUsingItem()) {
            entity.clearActiveItem()
            entity.setCharging(false)
        }
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    private fun fleeHandler(livingEntity: LivingEntity) {
        NoPenaltyTargeting.findFrom(this.entity, 16, 7, livingEntity.getPos())?.let { pos ->
            if (livingEntity.squaredDistanceTo(pos.x, pos.y, pos.z) > livingEntity.squaredDistanceTo(entity)) {
                if (entity.getNavigation().isIdle()) {
                    entity.navigation.startMovingTo(pos.x, pos.y, pos.z, speed * 1.25)
                }
            }
        }
    }

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
            if (entity.canAttack()) {
                this.targetHandler(livingEntity, shouldMoveTowardsTarget)
                val profession = (entity.getProfession() as Guard)
                profession.doWork(entity)
                squaredRange = profession.range
            } else {
                this.fleeHandler(livingEntity)
            }
        } else {
            // Talking with villagers
            this.targetHandler(livingEntity, distanceSquared > 3.0 + entity.random.nextInt(4).toDouble())
            if (unchargedTicksLeft <= -10) {
                unchargedTicksLeft = 0
                if (entity.random.nextInt(10) != 0) return
                this.fleeHandler(livingEntity)
                NoPenaltyTargeting.findFrom(this.entity, 16, 7, livingEntity.getPos())?.let { pos ->
                    (entity.getTarget() as CustomVillagerEntity).errand.push(Errand(BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt()), Action.MOVE))
                }
                NoPenaltyTargeting.findFrom(this.entity, 16, 7, livingEntity.getPos().add(10.0, 0.0, -11.0))?.let { pos ->
                    entity.errand.push(Errand(BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt()), Action.MOVE))
                }
                entity.setTarget(null)
                return
            }
            unchargedTicksLeft--
        }
    }
}
