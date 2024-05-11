package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.Hand
import java.util.EnumSet
import com.village.mod.LOGGER

class MeleeAttackGoal(entity: CustomVillagerEntity, private val speed: Double, private val pauseWhenMobIdle: Boolean) : Goal() {
    private val mob = entity
    private var path: Path? = null
    private var targetX = 0.0
    private var targetY = 0.0
    private var targetZ = 0.0
    private var updateCountdownTicks = 0
    private var cooldown = 0
    private val attackIntervalTicks = 20L
    private var lastUpdateTime: Long = 0

    init {
        controls = EnumSet.of(Control.MOVE, Control.LOOK)
    }

    override fun canStart(): Boolean {
        val currentTime = mob.world.time
        if (currentTime - lastUpdateTime < 40L) {
            return false
        }
        lastUpdateTime = currentTime
        val livingEntity = mob.target
        if (livingEntity == null || !livingEntity.isAlive) {
            return false
        }
        path = mob.navigation.findPathTo(livingEntity, 0)
        return path != null || mob.isInAttackRange(livingEntity)
    }

    override fun shouldContinue(): Boolean {
        val livingEntity = mob.target
        if (livingEntity == null || !livingEntity.isAlive) {
            return false
        }
        if (!pauseWhenMobIdle) {
            return !mob.navigation.isIdle
        }
        if (!mob.isInWalkTargetRange(livingEntity.blockPos)) {
            return false
        }
        return if (livingEntity is PlayerEntity) !livingEntity.isSpectator && !livingEntity.isCreative else true
    }

    override fun start() {
        mob.navigation.startMovingAlong(path, speed)
        mob.isAttacking = true
        updateCountdownTicks = 0
        cooldown = 0
    }

    override fun stop() {
        val livingEntity = mob.target
        if (livingEntity == null || EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
            mob.target = null
        }
        mob.isAttacking = false
        mob.navigation.stop()
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    override fun tick() {
        val livingEntity = mob.target ?: return
        mob.lookControl.lookAt(livingEntity, 30.0f, 30.0f)
        updateCountdownTicks = updateCountdownTicks.coerceAtLeast(0) - 1
        if ((pauseWhenMobIdle || mob.visibilityCache.canSee(livingEntity)) && updateCountdownTicks <= 0 &&
            (
                targetX == 0.0 && targetY == 0.0 && targetZ == 0.0 || livingEntity.squaredDistanceTo(
                    targetX,
                    targetY,
                    targetZ,
                ) >= 1.0 || mob.random.nextFloat() < 0.05f
                )
        ) {
            targetX = livingEntity.x
            targetY = livingEntity.y
            targetZ = livingEntity.z
            updateCountdownTicks = 4 + mob.random.nextInt(7)
            val distance = mob.squaredDistanceTo(livingEntity)
            if (distance > 1024.0) {
                updateCountdownTicks += 10
            } else if (distance > 256.0) {
                updateCountdownTicks += 5
            }
            if (!mob.navigation.startMovingTo(livingEntity, speed)) {
                updateCountdownTicks += 15
            }
            updateCountdownTicks = getTickCount(updateCountdownTicks)
        }
        cooldown = cooldown.coerceAtLeast(0) - 1
        attack(livingEntity)
    }

    protected fun attack(target: LivingEntity) {
        if (canAttack(target)) {
            resetCooldown()
            mob.swingHand(Hand.MAIN_HAND)
            mob.tryAttack(target)
        }
    }

    protected fun resetCooldown() {
        cooldown = getTickCount(20)
    }

    protected fun isCooledDown(): Boolean {
        return cooldown <= 0
    }

    protected fun canAttack(target: LivingEntity): Boolean {
        return isCooledDown() && mob.isInAttackRange(target) && mob.visibilityCache.canSee(target)
    }

    protected fun getCooldown(): Int {
        return cooldown
    }

    protected fun getMaxCooldown(): Int {
        return getTickCount(20)
    }
}
