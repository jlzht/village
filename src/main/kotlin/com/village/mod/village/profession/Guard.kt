package com.village.mod.village.profession

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.ItemPredicate
import com.village.mod.village.structure.StructureType
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import kotlin.math.sign

class Guard(villager: CustomVillagerEntity) : Profession(villager) {
    override val type = ProfessionType.GUARD
    override val desiredItems = ItemPredicate.WEAPON
    private var stage: Stage = Stage.UNLOADED
    private var chargedTicksLeft: Int = 0
    private var unchargedTicksLeft: Int = 10
    private var weapon: Weapon = Weapon.NONE
    private var arrowSlot: Int = -1
    private var arrowLookupDelay: Long = 0L
    var range: Float = 0.0f

    private fun takeWeapons(predicate: (Item) -> Boolean, range: Float): Boolean {
        val equipped = this.villager.getStackInHand(Hand.MAIN_HAND)
        if (predicate(equipped.getItem())) {
            return true
        }

        val item = this.villager.inventory.takeItem(predicate)
        if (item != ItemStack.EMPTY) {
            // val itemStack = this.villager.getStackInHand(Hand.MAIN_HAND)
            this.villager.inventory.tryInsert(equipped)
            this.villager.equipStack(EquipmentSlot.MAINHAND, item)
            LOGGER.info("taking weapons...")
            resetCharging()
            this.range = range
            return true
        }
        return false
    }

    override fun canWork(): Boolean {
        val ll = this.villager.pos.squaredDistanceTo(villager.target?.pos)
        return this.villager.health > 4.0f && this.checkWeapon(ll)
    }
    private fun hasArrows(): Boolean {
        val fixed = this.villager.inventory.getFixedField()
        if (fixed.get(1).isEmpty) {
            return false
        }
        return true
    }

    private fun checkWeapon(distanceSquared: Double): Boolean {
        if (stage != Stage.READY && !this.villager.isUsingItem) {
            if (hasArrows()) {
                if (distanceSquared >= 10.0f && takeWeapons(ItemPredicate.BOW, 180.0f)) {
                    weapon = Weapon.BOW; return true
                } else if (distanceSquared >= 10.0f && takeWeapons(ItemPredicate.CROSSBOW, 225.0f)) {
                    weapon = Weapon.CROSSBOW; return true
                }
            }
            if (takeWeapons(ItemPredicate.SWORD, 3.0f)) {
                weapon = Weapon.SWORD; return true
            }
            return false
        }
        return true
    }

    private fun deflateProjectiles(livingEntity: LivingEntity, distanceSquared: Double) {
        // add player check
        val aux = livingEntity.headYaw - this.villager.headYaw - 180
        val lookingAngle = if (Math.abs(aux) > 360) (Math.abs(aux) - 360) else Math.abs(aux)
        val reaction = (-sign((Math.sin((Math.PI / 30.0f) * lookingAngle))))
        LOGGER.info("REACTION: {} --- {} - {} - {}", reaction, lookingAngle, livingEntity.headYaw, this.villager.headYaw)
        if (distanceSquared <= 8.0f) { // was 12.0f
            this.villager.moveControl.strafeTo(if (stage == Stage.LOADING) { -0.5f } else { -1.25f }, 0.0f)
        } else if (Math.abs(lookingAngle) < 15.0f && !this.villager.isHoldingSword() && villager.target != null) {
            this.villager.moveControl.strafeTo(0.0f, (if (this.villager.isUsingItem()) { 0.5f } else { 1.25f }) * reaction.toFloat())
        }
    }

    private fun resetCharging() {
        stage = Stage.UNLOADED
        chargedTicksLeft = 0
        unchargedTicksLeft = 10
    }

    private fun attack(target: LivingEntity) {
        if (this.villager.getBoundingBox().expand(3.0, 1.0, 3.0).intersects(target.getBoundingBox()) && this.villager.getVisibilityCache().canSee(target)) {
            this.villager.swingHand(Hand.MAIN_HAND)
            this.villager.tryAttack(target)
        }
    }

    override fun doWork() {
        val livingEntity = villager.target ?: return
        val distance = villager.squaredDistanceTo(livingEntity)
        //this.checkWeapon(distance)
        this.deflateProjectiles(livingEntity, distance)
        when (stage) {
            Stage.UNLOADED -> {
                if (unchargedTicksLeft <= 0) {
                    when (weapon) {
                        Weapon.BOW -> villager.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(villager, Items.BOW))
                        Weapon.CROSSBOW -> {
                            val item = villager.getStackInHand(ProjectileUtil.getHandPossiblyHolding(villager, Items.CROSSBOW))
                            if (CrossbowItem.isCharged(item)) {
                                stage = Stage.READY
                                return
                            }
                            villager.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(villager, Items.CROSSBOW))
                        }
                        else -> {}
                    }
                    if (this.chargedTicksLeft <= 0) {
                        stage = Stage.LOADING
                        villager.setCharging(true)
                    }
                }
                unchargedTicksLeft--
            }
            Stage.LOADING -> {
                if (weapon != Weapon.SWORD && !villager.isUsingItem) {
                    stage = Stage.UNLOADED
                    return
                }
                val useTime = villager.getItemUseTime()
                val evaluete: Boolean = when (weapon) {
                    Weapon.CROSSBOW -> useTime >= CrossbowItem.getPullTime(villager.getActiveItem())
                    Weapon.BOW -> useTime >= 20
                    Weapon.SWORD -> true
                    else -> false
                }
                if (evaluete) {
                    stage = Stage.LOADED
                    chargedTicksLeft = 10 + villager.random.nextInt(10)
                    villager.setCharging(false)
                    if (weapon == Weapon.CROSSBOW) {
                        val item = villager.getStackInHand(ProjectileUtil.getHandPossiblyHolding(villager, Items.CROSSBOW))
                        CrossbowItem.setCharged(item, true)
                    }
                }
            }
            Stage.LOADED -> {
                chargedTicksLeft--
                if (chargedTicksLeft <= 0) {
                    stage = Stage.READY
                }
            }
            Stage.READY -> {
                if (villager.visibilityCache.canSee(livingEntity)) {
                    when (weapon) {
                        Weapon.CROSSBOW -> {
                            this.shoot(livingEntity, true)
                            this.villager.stopUsingItem()
                        }
                        Weapon.BOW -> {
                            this.shoot(livingEntity, false)
                            this.villager.stopUsingItem()
                        }
                        Weapon.SWORD -> this.attack(livingEntity)
                        else -> {}
                    }
                    resetCharging()
                }
            }
        }
    }

    private fun shoot(target: LivingEntity, isCrossbow: Boolean) {
        val itemStack = if (isCrossbow) {
            this.villager.getProjectileType(this.villager.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.villager, Items.CROSSBOW)))
        } else {
            this.villager.getProjectileType(this.villager.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.villager, Items.BOW)))
        }
        val projectileEntity: PersistentProjectileEntity = ProjectileUtil.createArrowProjectile(this.villager, itemStack, 1.0f)
        if (this.villager.random.nextFloat() < 0.2f) {
            projectileEntity.setCritical(true)
        }
        if (isCrossbow) {
            projectileEntity.setSound(SoundEvents.ITEM_CROSSBOW_HIT)
            projectileEntity.setShotFromCrossbow(true)
            val i: Byte = EnchantmentHelper.getLevel(Enchantments.PIERCING, itemStack).toByte()
            if (i > 0) projectileEntity.setPierceLevel(i)
            val test = this.villager.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.villager, Items.CROSSBOW))
            CrossbowItem.setCharged(test, false)
        } else {
        }
        this.shootTo(target, projectileEntity, 1.0f, 1.6f)
        this.villager.world.spawnEntity(projectileEntity)
        this.villager.inventory.getFixedField()[1].decrement(1)
    }

    fun shootTo(target: LivingEntity, projectile: ProjectileEntity, multishotSpray: Float, speed: Float) {
        val d = target.getX() - this.villager.getX()
        val e = target.getZ() - this.villager.getZ()
        val f = Math.sqrt(d * d + e * e)
        val g = target.getBodyY(0.3333333333333333) - projectile.getY() + f * 0.2F
        val vector3f = getProjectileLaunchVelocity(Vec3d(d, g, e), multishotSpray)
        projectile.setVelocity(vector3f.x().toDouble(), vector3f.y().toDouble(), vector3f.z().toDouble(), speed, 14 - this.villager.world.difficulty.id * 4.0f)
        this.villager.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f / (this.villager.random.nextFloat() * 0.4f + 0.8f))
    }

    private fun getProjectileLaunchVelocity(positionDelta: Vec3d, multishotSpray: Float): Vector3f {
        val vector3f = positionDelta.toVector3f().normalize()
        var vector3f2 = Vector3f(vector3f).cross(Vector3f(0.0f, 0.0f, 1.0f))
        if (vector3f2.lengthSquared() <= 1.0E-7) {
            val vec3d = this.villager.getOppositeRotationVector(1.0f)
            vector3f2 = Vector3f(vector3f).cross(vec3d.toVector3f())
        }
        val vector3f3 = vector3f.rotateAxis(0.0f, vector3f2.x, vector3f2.y, vector3f2.z)
        return vector3f.rotateAxis((Math.PI.toFloat() / 180), vector3f3.x, vector3f3.y, vector3f3.z)
    }

    enum class Weapon {
        CROSSBOW,
        SWORD,
        SPEAR,
        BOW,
        NONE,
    }

    enum class Stage {
        UNLOADED,
        LOADING,
        LOADED,
        READY,
    }

    override val structureInterest: StructureType = StructureType.NONE
}
