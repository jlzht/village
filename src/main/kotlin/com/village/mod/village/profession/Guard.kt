package com.village.mod.village.profession

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.InventoryUser
import com.village.mod.village.structure.StructureType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ArrowItem
import net.minecraft.item.BowItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.joml.Vector3f
import kotlin.math.sign

class Guard() : Profession() {
    override val type = ProfessionType.GUARD
    override val desiredItems: (Item) -> Boolean = { item -> item is SwordItem || item is BowItem || item is CrossbowItem }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
    private var stage: Stage = Stage.UNLOADED
    private var chargedTicksLeft: Int = 0
    private var unchargedTicksLeft: Int = 10
    private var weapon: Weapon = Weapon.NONE
    public var range: Float = 0.0f

    private fun takeWeapons(worker: CustomVillagerEntity, predicate: (Item) -> Boolean, rang: Float): Boolean {
        val equipped = worker.getStackInHand(Hand.MAIN_HAND)
        if (predicate(equipped.getItem())) {
            return true
        }
        val item = worker.takeItem(worker, predicate)
        if (item != ItemStack.EMPTY) {
            worker.equipStack(EquipmentSlot.MAINHAND, item)
            resetCharging()
            this.range = rang
            return true
        }
        return false
    }

    private fun checkWeapon(worker: CustomVillagerEntity, distanceSquared: Double) {
        if (stage != Stage.READY && !worker.isUsingItem) {
            if (distanceSquared >= 10.0f && takeWeapons(worker, InventoryUser.bowPredicate, 180.0f)) {
                weapon = Weapon.BOW; return
            } else if (distanceSquared >= 10.0f && takeWeapons(worker, InventoryUser.crossbowPredicate, 225.0f)) {
                weapon = Weapon.CROSSBOW; return
            }
            if (takeWeapons(worker, InventoryUser.swordPredicate, 3.0f)) {
                weapon = Weapon.SWORD; return
            }
        }
    }

    private fun deflateProjectiles(worker: CustomVillagerEntity, livingEntity: LivingEntity, distanceSquared: Double) {
        val lookingAngle = livingEntity.headYaw - worker.headYaw + 180
        val reaction = (-sign((Math.sin((Math.PI / 30.0f) * lookingAngle))))
        if (distanceSquared <= 12.0f) {
            worker.moveControl.strafeTo(if (stage == Stage.LOADING) { -0.5f } else { -1.25f }, 0.0f)
        } else if (Math.abs(lookingAngle) < 15.0f) {
            worker.moveControl.strafeTo(0.0f, (if (stage == Stage.LOADING) { 0.5f } else { 1.25f }) * reaction.toFloat())
        }
    }

    private fun resetCharging() {
        stage = Stage.UNLOADED
        chargedTicksLeft = 0
        unchargedTicksLeft = 10
    }

    fun attack(worker: CustomVillagerEntity, target: LivingEntity) {
        if (worker.getBoundingBox().expand(3.0, 1.0, 3.0).intersects(target.getBoundingBox()) && worker.getVisibilityCache().canSee(target)) {
            worker.swingHand(Hand.MAIN_HAND)
            worker.tryAttack(target)
        }
    }

    override fun doWork(worker: CustomVillagerEntity) {
        val livingEntity = worker.target ?: return
        val distance = worker.squaredDistanceTo(livingEntity)
        this.checkWeapon(worker, distance)
        this.deflateProjectiles(worker, livingEntity, distance)
        when (stage) {
            Stage.UNLOADED -> {
                if (unchargedTicksLeft <= 0) {
                    when (weapon) {
                        Weapon.BOW -> worker.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(worker, Items.BOW))
                        Weapon.CROSSBOW -> {
                            val item = worker.getStackInHand(ProjectileUtil.getHandPossiblyHolding(worker, Items.CROSSBOW))
                            if (CrossbowItem.isCharged(item)) {
                                stage = Stage.READY
                                return
                            }
                            worker.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(worker, Items.CROSSBOW))
                        }
                        else -> {}
                    }
                    if (this.chargedTicksLeft <= 0) {
                        stage = Stage.LOADING
                        worker.setCharging(true)
                    }
                }
                unchargedTicksLeft--
            }
            Stage.LOADING -> {
                if (weapon != Weapon.SWORD && !worker.isUsingItem) {
                    stage = Stage.UNLOADED
                    return
                }
                val useTime = worker.getItemUseTime()
                val evaluete: Boolean = when (weapon) {
                    Weapon.CROSSBOW -> useTime >= CrossbowItem.getPullTime(worker.getActiveItem())
                    Weapon.BOW -> useTime >= 20
                    Weapon.SWORD -> true
                    else -> false
                }
                if (evaluete) {
                    stage = Stage.LOADED
                    chargedTicksLeft = 10 + worker.random.nextInt(10)
                    worker.setCharging(false)
                    if (weapon == Weapon.CROSSBOW) {
                        val item = worker.getStackInHand(ProjectileUtil.getHandPossiblyHolding(worker, Items.CROSSBOW))
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
                if (worker.visibilityCache.canSee(livingEntity)) {
                    when (weapon) {
                        Weapon.CROSSBOW -> {
                            this.shoot(worker.world, worker, livingEntity, true)
                        }
                        Weapon.BOW -> {
                            this.shoot(worker.world, worker, livingEntity, false)
                        }
                        Weapon.SWORD -> this.attack(worker, livingEntity)
                        else -> {}
                    }
                    resetCharging()
                }
            }
        }
    }

    fun shoot(world: World, entity: CustomVillagerEntity, target: LivingEntity, isCrossbow: Boolean) {
        if (world.isClient) {
            LOGGER.info("this is useless")
            return
        }
        val arrowItem: ArrowItem = Items.ARROW as ArrowItem
        var itemStack = entity.getProjectileType(entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, if (entity.isHolding(Items.CROSSBOW)) { Items.CROSSBOW } else { Items.BOW })))
        val projectileEntity: PersistentProjectileEntity = arrowItem.createArrow(world, itemStack, entity)
        if (isCrossbow) {
            projectileEntity.setShotFromCrossbow(true)
            val itemStackr = entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW))
            CrossbowItem.setCharged(itemStackr, false)
        }
        this.shootTo(entity, target, projectileEntity, 1.0f, 1.6f)
        entity.stopUsingItem()
        world.spawnEntity(projectileEntity)
    }

    fun shootTo(entity: CustomVillagerEntity, target: LivingEntity, projectile: ProjectileEntity, multishotSpray: Float, speed: Float) {
        val d = target.getX() - entity.getX()
        val e = target.getZ() - entity.getZ()
        val f = Math.sqrt(d * d + e * e)
        val g = target.getBodyY(0.3333333333333333) - projectile.getY() + f * 0.2F
        val vector3f = getProjectileLaunchVelocity(entity, Vec3d(d, g, e), multishotSpray)
        projectile.setVelocity(vector3f.x().toDouble(), vector3f.y().toDouble(), vector3f.z().toDouble(), speed, 14 - entity.world.difficulty.id * 4.0f)
        // entity.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f / (entity.random.nextFloat() * 0.4f + 0.8f))
    }

    fun getProjectileLaunchVelocity(entity: LivingEntity, positionDelta: Vec3d, multishotSpray: Float): Vector3f {
        val vector3f = positionDelta.toVector3f().normalize()
        var vector3f2 = Vector3f(vector3f).cross(Vector3f(0.0f, 0.0f, 1.0f))
        if (vector3f2.lengthSquared() <= 1.0E-7) {
            val vec3d = entity.getOppositeRotationVector(1.0f)
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
