package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Guard
import com.village.mod.village.villager.State
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.RangedWeaponItem
import net.minecraft.item.SwordItem
import net.minecraft.util.Hand
import java.util.EnumSet
import kotlin.math.sign

class RangedAttackGoal(private val entity: CustomVillagerEntity, private val speed: Double) : Goal() {
    private var stage: Stage = Stage.UNLOADED
    private var seeingTargetTicker: Int = 0
    private var chargedTicksLeft: Int = 0
    private var unchargedTicksLeft: Int = 10
    private var squaredRange: Float = 0.0f

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (this.hasAliveTarget()) {
            return true
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        return this.hasAliveTarget()
    }

    private fun hasAliveTarget(): Boolean {
        return entity.target != null && entity.target!!.isAlive
    }

    override fun start() {
        entity.setState(State.ATTACK)
        entity.lockState()
    }

    override fun stop() {
        entity.setState(State.ALERT)
        entity.clearActiveItem()
        entity.unlockState()
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    // TODO: pass prefered arm
    private fun checkTools(desiredItem: Item, range: Float): Boolean {
        if (!entity.isHolding(desiredItem)) {
            val a = when (desiredItem) {
                is SwordItem -> entity.swordPredicate
                is RangedWeaponItem -> entity.rangedPredicate
                else -> entity.swordPredicate
            }

            val equipped = entity.getStackInHand(Hand.MAIN_HAND)
            if (equipped != ItemStack.EMPTY) {
                if (entity.inventory.canInsert(equipped)) {
                    val item = entity.takeItem(a)
                    if (item != ItemStack.EMPTY) {
                        entity.equipStack(EquipmentSlot.MAINHAND, item)
                        entity.getInventory().addStack(equipped)
                        squaredRange = range
                        LOGGER.info("TIME TO EQUIP ITEM")
                        return true
                    }
                }
            } else {
                val item = entity.takeItem(a)
                if (item != ItemStack.EMPTY) {
                    entity.equipStack(EquipmentSlot.MAINHAND, item)
                    LOGGER.info("I HAD EMPTY STACK")
                    return true
                } else {
                    LOGGER.info("DO NOT HAVE ITEM")
                    return false
                }
            }
        } else {
            squaredRange = range
            LOGGER.info("ALREADY HAVE")
            return false
        }
        return false
    }

    override fun tick() {
        val livingEntity = entity.target ?: return
        val canSeeTarget = entity.visibilityCache.canSee(livingEntity)
        if (canSeeTarget != seeingTargetTicker > 0) { seeingTargetTicker = 0 }
        seeingTargetTicker = if (canSeeTarget) { seeingTargetTicker + 1 } else { seeingTargetTicker - 1 }
        val distanceSquared = entity.squaredDistanceTo(livingEntity)
        LOGGER.info("DISTANCE: {} - STAGE: {}", distanceSquared, stage)
        if (stage != Stage.READY) {
            when {
                distanceSquared >= 30.0f ->
                    if (checkTools(Items.BOW, 180.0f)) {
                        LOGGER.info("I HAVE A BOW")
                        stage = Stage.UNLOADED
                        unchargedTicksLeft = 5
                    } else if (!entity.isHolding(Items.BOW)) {
                        if (checkTools(Items.CROSSBOW, 225.0f)) {
                            LOGGER.info("I HAVE A CROSSBOW")
                            stage = Stage.UNLOADED
                            unchargedTicksLeft = 5
                        }
                    }

                distanceSquared <= 6.0f -> if (checkTools(Items.WOODEN_SWORD, 2.50f)) {
                    LOGGER.info("I HAVE A SWORD")
                    stage = Stage.LOADED
                    unchargedTicksLeft = 5
                }
                // if true continue, if false set FLEE and stop
            }
        }

        val bl0 = entity.getStackInHand(Hand.MAIN_HAND).getItem() is SwordItem
        LOGGER.info("BODY:{}", livingEntity.headYaw - entity.headYaw + 180)
        LOGGER.info("ENTITY BODY:{}", livingEntity.headYaw - entity.headYaw - 360)
        val lookingAngle = livingEntity.headYaw - entity.headYaw + 180
        val reaction = (-sign((Math.sin((Math.PI / 30.0f) * lookingAngle))))
        if (distanceSquared <= 12.0f) {
            this.entity.moveControl.strafeTo(if (stage == Stage.LOADING) { -0.5f } else { -1.25f }, 0.0f)
        } else if (Math.abs(lookingAngle) < 15.0f && entity.getStackInHand(Hand.MAIN_HAND).getItem() is RangedWeaponItem) {
            this.entity.moveControl.strafeTo(0.0f, (if (stage == Stage.LOADING) { 0.5f } else { 1.25f }) * reaction.toFloat())
        }

        val shouldMoveTowardsTarget = (distanceSquared > squaredRange || seeingTargetTicker < 5)
        if (shouldMoveTowardsTarget) {
            if (bl0 && distanceSquared >= 5) {
                entity.navigation.startMovingTo(livingEntity, if (isUncharged()) speed * 1.25 else speed)
            } else {
                entity.navigation.startMovingTo(livingEntity, if (isUncharged()) speed else speed * 0.5)
            }
        } else {
            if (!bl0) {
                entity.navigation.stop()
            }
        }
        entity.lookControl.lookAt(livingEntity, 30.0f, 30.0f)
        entity.yaw = entity.headYaw

        when (stage) {
            Stage.UNLOADED -> {
                LOGGER.info("UNCHARGED:{} - CHARGED:{}", unchargedTicksLeft, chargedTicksLeft)
                if (unchargedTicksLeft <= 0) {
                    if (!shouldMoveTowardsTarget) {
                        if (!bl0) {
                            if (entity.isHolding(Items.CROSSBOW)) {
                                (entity as LivingEntity).setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.CROSSBOW))
                                val kes = this.entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.CROSSBOW))
                                if (CrossbowItem.isCharged(kes)) {
                                    stage = Stage.LOADED
                                    return
                                }
                            } else if (entity.isHolding(Items.BOW)) {
                                (entity as LivingEntity).setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.BOW))
                            }
                        }
                        if (this.chargedTicksLeft <= 0) {
                            stage = Stage.LOADING
                            entity.setCharging(true)
                        }
                    }
                }
                unchargedTicksLeft--
            }
            Stage.LOADING -> {
                if (!bl0) {
                    if (!entity.isUsingItem) {
                        stage = Stage.UNLOADED
                    }
                    val useTime = (entity as LivingEntity).getItemUseTime()
                    val zah = if (entity.isHolding(Items.CROSSBOW)) { CrossbowItem.getPullTime((this.entity as LivingEntity).getActiveItem()) } else { 0 }
                    if (useTime >= if (zah != 0) { zah } else { 20 }) {
                        stage = Stage.LOADED
                        if (zah != 0) {
                            (this.entity as LivingEntity).stopUsingItem()
                        }
                        chargedTicksLeft = 20 + entity.random.nextInt(20)
                        entity.setCharging(false)
                    }
                } else {
                    stage = Stage.LOADED
                    chargedTicksLeft = if (this.entity.squaredDistanceTo(livingEntity) > 5.0f) { 0 } else { 10 }
                }
            }
            Stage.LOADED -> {
                chargedTicksLeft--
                if (chargedTicksLeft <= 0) {
                    stage = Stage.READY
                }
            }
            Stage.READY -> {
                if (canSeeTarget) {
                    if (entity.isHolding(Items.BOW) || entity.isHolding(Items.CROSSBOW)) {
                        (entity.getProfession() as Guard).shoot(entity.world, entity, livingEntity)
                        // entity.stopUsingItem()
                        if (entity.isHolding(Items.CROSSBOW)) {
                            val itemStack2 = this.entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.entity, Items.CROSSBOW))
                            CrossbowItem.setCharged(itemStack2, false)
                        }
                    } else {
                        attack(livingEntity)
                    }
                    stage = Stage.UNLOADED
                    unchargedTicksLeft = 10
                }
            }
        }
    }
    protected fun attack(target: LivingEntity) {
        if (this.entity.getBoundingBox().expand(2.0, 0.0, 2.0).intersects(target.getBoundingBox()) && this.entity.getVisibilityCache().canSee(target)) {
            entity.swingHand(Hand.MAIN_HAND)
            entity.tryAttack(target)
        }
    }

    private fun isUncharged(): Boolean {
        return stage == Stage.UNLOADED
    }

    private fun isReady(): Boolean {
        return stage != Stage.UNLOADED
    }

    enum class Stage {
        UNLOADED,
        LOADING,
        LOADED,
        READY,
    }
}
