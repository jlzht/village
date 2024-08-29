package com.village.mod.action

import com.village.mod.LOGGER
import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.ItemPredicate
import com.village.mod.village.profession.Fisherman
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.PlantBlock
import net.minecraft.block.SlabBlock
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.BoneMealItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Items
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f

data class Errand(val cid: Action.Type, val pos: BlockPos) // make pos nullable

typealias Input = (entity: CustomVillagerEntity, pos: BlockPos?) -> Byte

sealed class Action {
    abstract val scan: Input // scan if errand can be picked
    abstract val test: Input // verify if errand can be executed
    abstract val exec: Input // execute lambda representing errand
    abstract val eval: Input // scan if errand can me end
    open val ticksToTest: Int = 5
    open val ticksToExec: Int = 5
    open val radiusToAct: Float = 4.0f // defines the proximity the entity needs to be to test and exec
    open val radiusToLook: Float = 4.0f // defines the proximity the entity needs to start looking at the target
    open val speedModifier: Double = 1.0
    open val shouldMoveTowards: (Double) -> Boolean = { d -> d > radiusToAct }
    val shouldLookAt: (Double) -> Boolean = { d -> d < radiusToLook }

    object Explore : Action() {
        override val scan: Input = { e, p -> 1 }
        override val test: Input = { _, _ -> 0 }
        override val exec: Input = { _, _ -> 1 }
        override val eval: Input = { e, p -> 0 }
    }

    object Sleep : Action() {
        override val scan: Input = { e, _ -> e.canSleep().toByte() }
        override val test: Input = { e, _ -> e.canSleep().toByte() }
        override val exec: Input = { e, p -> e.sleep(p!!); 1 }
        override val eval: Input = { e, _ -> (!e.canSleep()).toByte() }
    }

    object Till : Action() {
        override val scan: Input = { e, _ ->
            e.inventory.hasItem(ItemPredicate.HOE).toByte(5)
        }
        override val test: Input = { e, p ->
            (
                e.world.getBlockState(p!!.down()).block in TILLABLE_BLOCKS &&
                    e.inventory.tryEquip(ItemPredicate.HOE, EquipmentSlot.MAINHAND)
                ).toByte()
        }
        override val exec: Input = { e, p ->
            val stack = e.getStackInHand(Hand.MAIN_HAND)
            e.swingHand(Hand.MAIN_HAND)
            e.world.setBlockState(p, Blocks.FARMLAND.defaultState, Block.NOTIFY_NEIGHBORS)
            stack.damage(1, e, { j -> j.sendToolBreakStatus(Hand.MAIN_HAND) })
            1
        }
        override val eval: Input = { _, _ -> 1 }
    }

    object Plant : Action() {
        override val exec: Input = { e, p ->
            val stack = e.getStackInHand(Hand.MAIN_HAND)
            when (stack.item) {
                Items.WHEAT_SEEDS -> Blocks.WHEAT.defaultState
                Items.BEETROOT_SEEDS -> Blocks.BEETROOTS.defaultState
                Items.CARROT -> Blocks.CARROTS.defaultState
                Items.POTATO -> Blocks.POTATOES.defaultState
                else -> null
            }?.let { block ->
                e.world.setBlockState(p!!.up(), block, Block.NOTIFY_LISTENERS)
                e.swingHand(Hand.MAIN_HAND)
                stack.decrement(1)
            }
            1
        }
        override val test: Input = { e, p ->
            (
                e.world.getBlockState(p!!.up()).isAir &&
                    e.world.getBlockState(p).block is FarmlandBlock &&
                    e.inventory.tryEquip(ItemPredicate.PLANTABLE, EquipmentSlot.MAINHAND)
                ).toByte()
        }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.PLANTABLE).toByte(5) }
    }

    object Powder : Action() {
        override val exec: Input = { e, p ->
            val stack = e.getStackInHand(Hand.MAIN_HAND)
            LOGGER.info("BONED")
            BoneMealItem.useOnFertilizable(e.getStackInHand(Hand.MAIN_HAND), e.world, p!!.up())
            e.swingHand(Hand.MAIN_HAND)
            stack.decrement(1)
            1
        }
        override val test: Input = { e, p ->
            p?.let {
                val state = e.world.getBlockState(it.up())
                (
                    state.block is CropBlock && !(state.block as CropBlock).isMature(state) &&
                        e.world.getBlockState(it).block is FarmlandBlock &&
                        e.inventory.tryEquip({ item -> item == Items.BONE_MEAL }, EquipmentSlot.MAINHAND)
                    ).toByte()
            } ?: 0
        }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { e, _ ->
            e.inventory.hasItem({ item -> item == Items.BONE_MEAL }).toByte(5)
        }
    }

    object Harvest : Action() {
        override val scan: Input = { _, _ -> 5 }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p!!.up()).block is CropBlock).toByte()
        }
        override val exec: Input = { entity, pos ->
            entity.world.breakBlock(pos, true)
            entity.swingHand(Hand.MAIN_HAND)
            1
        }
        override val eval: Input = { _, _ -> 1 }
    }

    object Break : Action() {
        override val exec: Input = { e, p ->
            e.world.breakBlock(p!!, true)
            e.swingHand(Hand.MAIN_HAND)
            1
        }
        override val test: Input = { e, p ->
            val block = e.world.getBlockState(p).block
            (block is CropBlock || block is PlantBlock).toByte()
        }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { _, _ -> 2 }
    }

    object Dig : Action() {
        override val exec: Input = { e, p ->
            if (e.world.getBlockState(p).block == Blocks.DIRT) {
                e.world.breakBlock(p, true)
                e.swingHand(Hand.MAIN_HAND)
            }
            0
        }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).block == Blocks.DIRT).toByte()
        }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.SHOVEL).toByte(5) }
    }

    object Mine : Action() {
        override val exec: Input = { e, p ->
            e.world.breakBlock(p, true)
            e.swingHand(Hand.MAIN_HAND)
            0
        }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).block == Blocks.STONE).toByte()
        }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.PICKAXE).toByte(5) }
    }

    object Fish : Action() {
        override val exec: Input = { e, _ ->
            // TODO: calculate velocity and angle to throw fishing bobber as pos
            e.world.spawnEntity(SimpleFishingBobberEntity(e, e.world, 0, 0))
            e.swingHand(Hand.MAIN_HAND)
            0
        }

        override val test: Input = { e, p ->
            val fisherman = (e.getProfession() as Fisherman)
            (
                NEIGHBOURS.all { e.world.getBlockState(p!!.add(it)).block == Blocks.WATER } &&
                    fisherman.getFishHook() == null &&
                    e.inventory.tryEquip(ItemPredicate.FISHING_ROD, EquipmentSlot.MAINHAND)
                ).toByte()
        }

        override val eval: Input = { e, _ ->
            // TODO: eval check to make fisherman look at block target
            val fisherman = (e.getProfession() as Fisherman)
            if (fisherman.getFishHook() == null) {
                e.swingHand(Hand.MAIN_HAND); 1
            } else {
                0
            }
        }

        override val scan: Input = { e, _ -> (e.inventory.hasItem(ItemPredicate.FISHING_ROD)).toByte(4) }
    }

    object Sit : Action() {
        override val exec: Input = { e, p ->
            e.sit(p!!)
            0
        }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).block is SlabBlock).toByte()
        }
        override val eval: Input = { _, _ -> 0 }
        override val scan: Input = { _, _ -> 1 }
    }

    object Flee : Action() {
        override val exec: Input = { _, _ -> 1 }
        override val test: Input = { _, _ -> 1 }
        override val eval: Input = { _, _ -> 1 }
        override val scan: Input = { _, _ -> 9 }
        override val radiusToAct: Float = 5.0f
        override val ticksToTest: Int = 1
        override val ticksToExec: Int = 1
        override val speedModifier: Double = 1.2
    }

    object Reach : Action() {
        override val scan: Input = { _, _ -> 8 }
        override val test: Input = { _, _ -> 1 }
        override val exec: Input = { _, _ -> 1 }
        override val eval: Input = { _, _ -> 1 }
        override val radiusToAct: Float = 8.0f
    }

    object Eat : Action() {
        override val scan: Input = { e, _ ->
            // TODO: add checks that increases priority like health and hunger bar
            e.inventory.hasItem(ItemPredicate.EDIBLE).toByte(2)
        }
        override val test: Input = { e, _ ->
            (e.inventory.tryEquip(ItemPredicate.EDIBLE, EquipmentSlot.OFFHAND)).toByte()
        }
        override val exec: Input = { e, _ -> e.setCurrentHand(Hand.OFF_HAND); 1 }
        override val eval: Input = { e, _ -> (!e.isEating()).toByte().also { LOGGER.info("I AM EAT:{}", it) } }
    }

    object Store : Action() {
        override val scan: Input = { _, _ -> 9 }
        override val test: Input = { _, _ -> 1 }
        override val exec: Input = { _, _ -> 1 }
        override val eval: Input = { _, _ -> 1 }
    }

    object Shoot : Action() {
        // has alive target, the target is far enough and have Arrows
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.CROSSBOW, EquipmentSlot.MAINHAND) &&
                    e.target != null
                ).toByte(9)
        }
        override val test: Input = { e, _ ->
            var ret: Byte = 0
            if (!(e.isUsingItem) && e.target != null) {
                val item = e.getStackInHand(ProjectileUtil.getHandPossiblyHolding(e, Items.CROSSBOW))
                if (!CrossbowItem.isCharged(item)) {
                    e.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(e, Items.CROSSBOW))
                    e.setCharging(true)
                }
                ret = 1
            }
            ret
        }
        override val exec: Input = { _, _ -> 1 }
        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            e.target?.let { t ->
                if (t.isAlive) {
                    if (e.isCharging() && e.getItemUseTime() >= CrossbowItem.getPullTime(e.getActiveItem()) + 10) {
                        val item = e.getStackInHand(ProjectileUtil.getHandPossiblyHolding(e, Items.CROSSBOW))
                        CrossbowItem.setCharged(item, true)
                        e.setCharging(false)
                    }
                    if (!e.isCharging()) {
                        if (e.random.nextInt(10) == 0) {
                            shoot(e, t, true)
                            e.stopUsingItem()
                            ret = 1
                        }
                    }
                } else {
                    if (e.isCharging() && e.getItemUseTime() >= CrossbowItem.getPullTime(e.getActiveItem())) {
                        val item = e.getStackInHand(ProjectileUtil.getHandPossiblyHolding(e, Items.CROSSBOW))
                        CrossbowItem.setCharged(item, true)
                        e.setCharging(false)
                    }
                    e.stopUsingItem()
                    ret = 1
                }
            } ?: run {
                if (e.isCharging() && e.getItemUseTime() >= CrossbowItem.getPullTime(e.getActiveItem())) {
                    val item = e.getStackInHand(ProjectileUtil.getHandPossiblyHolding(e, Items.CROSSBOW))
                    CrossbowItem.setCharged(item, true)
                    e.setCharging(false)
                }
                e.stopUsingItem()
                ret = 1
            }
            ret
        }
        override val ticksToExec: Int = 15
        override val ticksToTest: Int = 1
        override val radiusToAct: Float = 25.0f
        override val radiusToLook: Float = 18.0f

        private fun shoot(entity: CustomVillagerEntity, target: LivingEntity, isCrossbow: Boolean) {
            val itemStack = if (isCrossbow) {
                entity.getProjectileType(entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW)))
            } else {
                entity.getProjectileType(entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.BOW)))
            }
            val projectileEntity: PersistentProjectileEntity = ProjectileUtil.createArrowProjectile(entity, itemStack, 1.0f)
            if (entity.random.nextFloat() < 0.10f) {
                projectileEntity.setCritical(true)
            }
            if (isCrossbow) {
                projectileEntity.setSound(SoundEvents.ITEM_CROSSBOW_HIT)
                projectileEntity.setShotFromCrossbow(true)
                val i: Byte = EnchantmentHelper.getLevel(Enchantments.PIERCING, itemStack).toByte()
                if (i > 0) projectileEntity.setPierceLevel(i)
                val test = entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW))
                CrossbowItem.setCharged(test, false)
            } else {
            }
            this.shootTo(entity, target, projectileEntity, 1.0f, 1.6f)
            entity.world.spawnEntity(projectileEntity)
        }

        fun shootTo(entity: CustomVillagerEntity, target: LivingEntity, projectile: ProjectileEntity, multishotSpray: Float, speed: Float) {
            val d = target.getX() - entity.getX()
            val e = target.getZ() - entity.getZ()
            val f = Math.sqrt(d * d + e * e)
            val g = target.getBodyY(0.3333333333333333) - projectile.getY() + f * 0.2F
            val vector3f = getProjectileLaunchVelocity(entity, Vec3d(d, g, e), multishotSpray)
            projectile.setVelocity(vector3f.x().toDouble(), vector3f.y().toDouble(), vector3f.z().toDouble(), speed, 14 - entity.world.difficulty.id * 4.0f)
            entity.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f / (entity.random.nextFloat() * 0.4f + 0.8f))
        }

        private fun getProjectileLaunchVelocity(entity: CustomVillagerEntity, positionDelta: Vec3d, multishotSpray: Float): Vector3f {
            val vector3f = positionDelta.toVector3f().normalize()
            var vector3f2 = Vector3f(vector3f).cross(Vector3f(0.0f, 0.0f, 1.0f))
            if (vector3f2.lengthSquared() <= 1.0E-7) {
                val vec3d = entity.getOppositeRotationVector(1.0f)
                vector3f2 = Vector3f(vector3f).cross(vec3d.toVector3f())
            }
            val vector3f3 = vector3f.rotateAxis(0.0f, vector3f2.x, vector3f2.y, vector3f2.z)
            return vector3f.rotateAxis((Math.PI.toFloat() / 180), vector3f3.x, vector3f3.y, vector3f3.z)
        }
    }

    object Attack : Action() {
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.SWORD, EquipmentSlot.MAINHAND) &&
                    e.target != null
                ).toByte(9)
        }
        override val test: Input = { _, _ -> 1 }
        override val exec: Input = { _, _ -> 1 }
        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            e.target?.let { t ->
                if (t.isAlive) {
                    e.swingHand(Hand.MAIN_HAND)
                    e.tryAttack(t)
                }
                ret = 1
            } ?: run {
                ret = 1
            }
            ret
        }
        override val speedModifier: Double = 1.2
        override val radiusToAct: Float = 3.5f
        override val radiusToLook: Float = 12.0f
    }

    enum class Type {
        EXPLORE,
        SLEEP,
        TILL,
        PLANT,
        POWDER,
        HARVEST,
        BREAK,
        DIG,
        MINE,
        FISH,
        SIT,
        FLEE,
        REACH,
        EAT,
        STORE,
        SHOOT,
        ATTACK,
    }

    companion object {
        private val map = mapOf(
            Type.EXPLORE to Explore,
            Type.SLEEP to Sleep,
            Type.TILL to Till,
            Type.PLANT to Plant,
            Type.POWDER to Powder,
            Type.HARVEST to Harvest,
            Type.BREAK to Break,
            Type.DIG to Dig,
            Type.MINE to Mine,
            Type.FISH to Fish,
            Type.SIT to Sit,
            Type.FLEE to Flee,
            Type.REACH to Reach,
            Type.EAT to Eat,
            Type.STORE to Store,
            Type.SHOOT to Shoot,
            Type.ATTACK to Attack,
        )

        fun get(type: Type): Action = map[type] ?: Explore
        // TODO: puts these on a general utils object
        fun Boolean.toByte(mult: Int): Byte = if (this) (1 * mult).toByte() else 0
        fun Boolean.toByte(): Byte = if (this) 1 else 0
        private val NEIGHBOURS = setOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
        private val TILLABLE_BLOCKS = setOf(Blocks.DIRT_PATH, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT)
    }
}
