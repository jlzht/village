package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Guard
import com.village.mod.village.villager.State
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.Items
import net.minecraft.util.TimeHelper
import net.minecraft.util.math.intprovider.UniformIntProvider
import java.util.EnumSet

class RangedAttackGoal(private val entity: CustomVillagerEntity, private val speed: Double, private val range: Float) : Goal() {
    val COOLDOWN_RANGE: UniformIntProvider = TimeHelper.betweenSeconds(1, 2)
    private var stage: Stage = Stage.UNCHARGED
    private var seeingTargetTicker: Int = 0
    private var chargedTicksLeft: Int = 0
    private var squaredRange: Float = range * range
    private var cooldown: Int = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        return this.hasAliveTarget()
    }

    override fun shouldContinue(): Boolean {
        return this.canStart() && entity.isHolding(Items.CROSSBOW)
    }

    private fun hasAliveTarget(): Boolean {
        return entity.target != null && entity.target!!.isAlive && !entity.isState(State.SLEEP)
    }

    override fun start() {
        entity.setState(State.ATTACK)
        entity.lockState()
        //  if (!entity.isHolding(Items.CROSSBOW)) {
        // LOGGER.info("not holding")
        // val equipped = entity.getStackInHand(Hand.MAIN_HAND)
        // entity.equipStack(EquipmentSlot.MAINHAND, ItemStack(Items.CROSSBOW))
        // entity.inventory.addStack(equipped)
        //  }
    }

    override fun stop() {
        if (isReady() && entity.target == null) {
            entity.clearActiveItem()
            entity.setState(State.IDLE)
        }
        // entity.setState(State.IDLE)
        // entity.unlockState()
        // if (entity.isHolding(Items.CROSSBOW)) {
        // val equipped = entity.getStackInHand(Hand.MAIN_HAND)
        // if (entity.inventory.canInsert(equipped)) {
        //    entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        //    entity.inventory.addStack(equipped)
        // }
        // }
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    override fun tick() {
        val livingEntity = entity.target
        if (livingEntity == null) {
            return
        }
        val canSeeTarget = entity.visibilityCache.canSee(livingEntity)
        val seeingTarget = seeingTargetTicker > 0
        if (canSeeTarget != seeingTarget) {
            seeingTargetTicker = 0
        }
        seeingTargetTicker = if (canSeeTarget) seeingTargetTicker + 1 else seeingTargetTicker - 1
        val distanceSquared = entity.squaredDistanceTo(livingEntity)
        val shouldMoveTowardsTarget = (distanceSquared > squaredRange || seeingTargetTicker < 5) && chargedTicksLeft == 0
        if (shouldMoveTowardsTarget) {
            cooldown--
            if (cooldown <= 0) {
                entity.navigation.startMovingTo(livingEntity, if (isUncharged()) speed else speed * 0.5)
                cooldown = 5
            }
        } else {
            cooldown = 0
            entity.navigation.stop()
        }
        entity.lookControl.lookAt(livingEntity, 30.0f, 30.0f)
        when (stage) {
            Stage.UNCHARGED -> {
                if (!shouldMoveTowardsTarget) {
                    // (entity as LivingEntity).setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.CROSSBOW))
                    (entity as LivingEntity).setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.BOW))
                    stage = Stage.CHARGING
                    entity.setCharging(true)
                }
            }
            Stage.CHARGING -> {
                val itemStack = (entity as LivingEntity).activeItem
                val useTime = (entity as LivingEntity).getItemUseTime()
                if (!entity.isUsingItem) {
                    stage = Stage.UNCHARGED
                }
                // if (useTime >= CrossbowItem.getPullTime(itemStack)) {
                if (useTime >= 35) {
                    entity.stopUsingItem()
                    stage = Stage.CHARGED
                    // chargedTicksLeft = 20 + entity.random.nextInt(20)
                    entity.setCharging(false)
                }
            }
            Stage.CHARGED -> {
                // chargedTicksLeft--
                // if (chargedTicksLeft == 0) {
                stage = Stage.READY_TO_ATTACK
                // }
            }
            Stage.READY_TO_ATTACK -> {
                if (canSeeTarget) {
                    // LOGGER.info("IS READY LOL!")
                    // (entity as RangedAttackMob).shootAt(livingEntity, 1.0f)
                    // (entity.profession as Guard).shootAt(entity, 1.0f)
                    entity.clearActiveItem()
                    (entity.getProfession() as Guard).shoot(entity.world, entity, livingEntity)
                    // val itemStack2 = entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW))
                    // CrossbowItem.setCharged(itemStack2, false)
                    stage = Stage.UNCHARGED
                }
            }
        }
    }

    private fun isUncharged(): Boolean {
        return stage == Stage.UNCHARGED
    }

    private fun isReady(): Boolean {
        return stage != Stage.UNCHARGED
    }

    enum class Stage {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK,
    }
}
