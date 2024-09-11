package com.village.mod.action

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.ItemPredicate
import com.village.mod.util.BlockIterator
import com.village.mod.util.Finder
import com.village.mod.utils.CombatUtils
import com.village.mod.village.profession.Fisherman
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.CropBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.PlantBlock
import net.minecraft.block.SlabBlock
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.BoneMealItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Items
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

data class Errand(
    val cid: Action.Type,
    val pos: BlockPos?,
)

typealias Input = (entity: CustomVillagerEntity, pos: BlockPos?) -> Byte

// sealed class Combat : Action()
// sealed class Manage : Action()
// sealed class Change : Action()
// sealed class Toward : Action()

sealed class Action {
    abstract val scan: Input // scan if errand can be picked,
    open val test: Input = { _, _ -> 1 } // verify if errand can be executed
    open val exec: Input = { _, _ -> 1 } // execute lambda representing errand
    open val eval: Input = { _, _ -> 1 } // scan if errand can me end
    open val redo: Input = { _, _ -> 1 } // check if action must be redone

    open val ticksToTest: Int = 5
    open val ticksToExec: Int = 5
    open val radiusToAct: Float = 4.0f
    open val radiusToLook: Float = 4.0f
    open val speedModifier: Double = 1.0

    val shouldTest: (Int) -> Boolean = { c -> c >= ticksToTest }
    val shouldExec: (Int) -> Boolean = { c -> c >= ticksToExec }
    val shouldMove: (Double) -> Boolean = { d -> d > radiusToAct }
    val shouldLook: (Double) -> Boolean = { d -> d < radiusToLook }

    object Pick : Action() {
        override val scan: Input = { _, _ -> 3 }
        override val radiusToAct: Float = 2.3f
        override val radiusToLook: Float = 5.0f
        override val ticksToTest: Int = 5
        override val ticksToExec: Int = 2
    }

    object Sleep : Action() {
        override val scan: Input = { e, _ -> e.canSleep().toByte() }
        override val test: Input = { e, _ -> e.canSleep().toByte() }
        override val exec: Input = { e, p ->
            e.sleep(p!!)
            1
        }
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
            e.world.playSound(e, p, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f)
            stack.damage(1, e, { j -> j.sendToolBreakStatus(Hand.MAIN_HAND) })
            1
        }
    }

    object Plant : Action() {
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.PLANTABLE).toByte(5) }
        override val test: Input = { e, p ->
            (
                e.world.getBlockState(p!!.up()).isAir &&
                    e.world.getBlockState(p).block is FarmlandBlock &&
                    e.inventory.tryEquip(ItemPredicate.PLANTABLE, EquipmentSlot.MAINHAND)
            ).toByte()
        }

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
    }

    object Powder : Action() {
        override val scan: Input = { e, _ ->
            e.inventory.hasItem({ item -> item == Items.BONE_MEAL }).toByte(5)
        }

        override val test: Input = { e, p ->
            p?.let {
                val state = e.world.getBlockState(it.up())
                (
                    state.block is CropBlock &&
                        !(state.block as CropBlock).isMature(state) &&
                        e.world.getBlockState(it).block is FarmlandBlock &&
                        e.inventory.tryEquip({ item -> item == Items.BONE_MEAL }, EquipmentSlot.MAINHAND)
                ).toByte()
            } ?: 0
        }

        override val exec: Input = { e, p ->
            val stack = e.getStackInHand(Hand.MAIN_HAND)
            BoneMealItem.useOnFertilizable(e.getStackInHand(Hand.MAIN_HAND), e.world, p!!.up())
            e.swingHand(Hand.MAIN_HAND)
            stack.decrement(1)
            1
        }
    }

    object Harvest : Action() {
        override val scan: Input = { _, _ -> 5 }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p!!.up()).block is CropBlock).toByte()
        }
        override val exec: Input = { entity, pos ->
            entity.world.breakBlock(pos!!.up(), true)
            entity.swingHand(Hand.MAIN_HAND)
            1
        }
    }

    object Break : Action() {
        override val scan: Input = { _, _ -> 2 }
        override val test: Input = { e, p ->
            val block = e.world.getBlockState(p).block
            (block is CropBlock || block is PlantBlock).toByte()
        }
        override val exec: Input = { e, p ->
            e.world.breakBlock(p!!, true)
            e.swingHand(Hand.MAIN_HAND)
            1
        }
    }

    object Dig : Action() {
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.SHOVEL).toByte(5) }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).block == Blocks.DIRT).toByte()
        }
        override val exec: Input = { e, p ->
            if (e.world.getBlockState(p).block == Blocks.DIRT) {
                e.world.breakBlock(p, true)
                e.swingHand(Hand.MAIN_HAND)
            }
            0
        }
    }

    object Mine : Action() {
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.PICKAXE).toByte(5) }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).block == Blocks.STONE).toByte()
        }
        override val exec: Input = { e, p ->
            e.world.breakBlock(p, true)
            e.swingHand(Hand.MAIN_HAND)
            0
        }
    }

    object Fish : Action() {
        override val scan: Input = { e, _ -> e.inventory.hasItem(ItemPredicate.FISHING_ROD).toByte(4) }

        override val test: Input = { e, p ->
            val fisherman = (e.getProfession() as Fisherman)
            (
                BlockIterator.BOTTOM(p!!).all { e.world.getBlockState(p).block == Blocks.WATER } &&
                    fisherman.getFishHook() == null &&
                    e.inventory.tryEquip(ItemPredicate.FISHING_ROD, EquipmentSlot.MAINHAND)
            ).toByte()
        }

        override val exec: Input = { e, _ ->
            // TODO: calculate velocity and angle to throw fishing bobber as pos
            e.world.spawnEntity(SimpleFishingBobberEntity(e, e.world, 0, 0))
            e.swingHand(Hand.MAIN_HAND)
            1
        }

        override val eval: Input = { e, _ ->
            // TODO: eval check to make fisherman look at block target
            val fisherman = (e.getProfession() as Fisherman)
            if (fisherman.getFishHook() == null) {
                e.swingHand(Hand.MAIN_HAND)
                1
            } else {
                0
            }
        }
        override val radiusToAct: Float = 20.0f
        override val radiusToLook: Float = 20.0f
        override val ticksToTest: Int = 1
        override val ticksToExec: Int = 10
    }

    object Sit : Action() {
        override val scan: Input = { _, _ -> -1 }

        override val test: Input = { e, p ->
            var ret: Byte = 0
            if (p != null) {
                (e.world.getBlockState(p).block is SlabBlock).toByte()
            } else {
                ret = 1
            }
            ret
        }

        override val exec: Input = { e, p ->
            e.sit(p)
            1
        }
        override val eval: Input = { _, _ -> 1 } // hummm
    }

    object Flee : Action() {
        override val scan: Input = { _, _ -> 9 }
        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            e.target?.let { t ->
                if (e.squaredDistanceTo(t) > 24 && !t.canSee(e)) {
                    e.target = null
                    ret = 1
                } else {
                    ret = 2
                }
            } ?: run { ret = 1 }
            ret
        }
        override val redo: Input = { e, _ ->
            e.target?.let { t ->
                Finder.findFleeBlock(e, t)
            }
            1
        }
        override val radiusToAct: Float = 4.5f
        override val radiusToLook: Float = -1.0f
        override val ticksToTest: Int = 1
        override val ticksToExec: Int = 1
        override val speedModifier: Double = 1.33
    }

    object Move : Action() {
        override val scan: Input = { _, _ -> 8 }
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
        override val exec: Input = { e, _ ->
            e.setCurrentHand(Hand.OFF_HAND)
            1
        }
        override val eval: Input = { e, _ -> (!e.isEating()).toByte() }
    }

    object Store : Action() {
        override val scan: Input = { _, _ -> -1 }
        override val exec: Input = { e, _ ->
            e.target = null
            1
        }
        override val speedModifier: Double = 1.2
        override val radiusToLook: Float = -1.0f
    }

    object Charge : Action() {
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.CROSSBOW, EquipmentSlot.MAINHAND) &&
                    e.target != null
            ).toByte(9)
        }
        override val test: Input = { e, _ ->
            val item = e.getStackInHand(Hand.MAIN_HAND)
            if (!CrossbowItem.isCharged(item)) {
                e.setCurrentHand(Hand.MAIN_HAND)
            }
            1
        }

        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            val ready = e.getItemUseTime() >= CrossbowItem.getPullTime(e.getActiveItem())
            val item = e.getStackInHand(Hand.MAIN_HAND)
            e.target?.let { t ->
                if (t.isAlive) {
                    if (e.isUsingItem) {
                        if (ready) {
                            e.stopUsingItem()
                            CrossbowItem.setCharged(item, true)
                        }
                    } else if (e.random.nextInt(5) == 0) {
                        CombatUtils.shoot(e, t, item, Hand.MAIN_HAND, true)
                        ret = 1
                    }
                } else {
                    if (e.isUsingItem) {
                        if (ready) {
                            e.stopUsingItem()
                            CrossbowItem.setCharged(item, true)
                            ret = 1
                        }
                    } else {
                        ret = 1
                    }
                }
            } ?: run {
                ret = 1
            }
            ret
        }
        override val ticksToExec: Int = 10
        override val ticksToTest: Int = 2
        override val radiusToAct: Float = 175.0f
        override val radiusToLook: Float = 175.0f
    }

    object Attack : Action() {
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.SWORD, EquipmentSlot.MAINHAND) &&
                    e.target != null
            ).toByte(9)
        }
        override val eval: Input = { e, _ ->
            e.target?.let { t ->
                if (t.isAlive) {
                    e.swingHand(Hand.MAIN_HAND)
                    if (e.squaredDistanceTo(t) <= 4.0f) {
                        e.tryAttack(t)
                    }
                }
            }
            1
        }
        override val speedModifier: Double = 1.2
        override val radiusToAct: Float = 3.5f
        override val radiusToLook: Float = 12.0f
    }

    object Look : Action() {
        override val scan: Input = { _, _ -> 4 }
        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            if (e.random.nextInt(10) == 0 && e.target != null) {
                e.target = null
                ret = 1
            }
            ret
        }
        override val radiusToAct: Float = 75.0f
        override val radiusToLook: Float = 75.0f
        override val ticksToTest: Int = 1
        override val ticksToExec: Int = 20
    }

    object Open : Action() {
        override val scan: Input = { _, _ -> 10 }
        override val test: Input = { e, p ->
            (e.world.getBlockState(p).getBlock() is DoorBlock).toByte()
        }
        override val exec: Input = { e, p ->
            val state = e.world.getBlockState(p)
            val block = (state.getBlock() as DoorBlock)
            if (!block.isOpen(state)) {
                block.setOpen(e, e.getWorld(), state, p, true)
                e.swingHand(Hand.MAIN_HAND)
            }
            1
        }
        override val eval: Input = { e, p ->
            p?.let {
                val state = e.world.getBlockState(p)
                val d = state.get(DoorBlock.FACING)
                val r = e.blockPos.getSquaredDistance(p.offset(d).toCenterPos())
                val l = e.blockPos.getSquaredDistance(p.offset(d.getOpposite()).toCenterPos())
                e.getErrandsManager().add(
                    Action.Type.CLOSE,
                    if (r > l) {
                        p.offset(d)
                    } else {
                        p.offset(d.getOpposite())
                    },
                )
            }
            1
        }
    }

    object Close : Action() {
        override val scan: Input = { _, _ -> 9 }

        override val exec: Input = { e, p ->
            BlockIterator.NEIGHBOURS(p!!).find { e.world.getBlockState(it).getBlock() is DoorBlock }?.let { t ->
                val state = e.world.getBlockState(t)
                if (state.getBlock() is DoorBlock) {
                    (state.getBlock() as DoorBlock).setOpen(e, e.getWorld(), state, t, false)
                    e.swingHand(Hand.MAIN_HAND)
                }
            }
            1
        }

        override val radiusToAct: Float = 2.3f
        override val ticksToExec: Int = 5
        override val ticksToTest: Int = 2
    }

    object Defend : Action() {
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.SHIELD, EquipmentSlot.OFFHAND) &&
                    e.target != null
            ).toByte(10)
        }
        override val test: Input = { e, _ ->
            e.setCurrentHand(Hand.OFF_HAND)
            1
        }
        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            e.target?.let { t ->
                if (e.random.nextInt(20) == 0) {
                    e.stopUsingItem()
                    ret = 1
                }
                if (!t.isAlive) {
                    e.stopUsingItem()
                    ret = 1
                }
            } ?: run {
                e.stopUsingItem()
                ret = 1
            }
            ret
        }
    }

    object Aim : Action() {
        override val scan: Input = { e, _ ->
            (
                e.inventory.tryEquip(ItemPredicate.BOW, EquipmentSlot.MAINHAND) &&
                    e.target != null
            ).toByte(9)
        }

        override val test: Input = { e, _ ->
            e.setCurrentHand(Hand.MAIN_HAND)
            1
        }

        override val eval: Input = { e, _ ->
            var ret: Byte = 0
            val ready = e.getItemUseTime() >= 25
            val item = e.getStackInHand(Hand.MAIN_HAND)
            e.target?.let { t ->
                if (t.isAlive) {
                    if (ready && e.random.nextInt(5) == 0) {
                        CombatUtils.shoot(e, t, item, Hand.MAIN_HAND, false)
                        e.stopUsingItem()
                        ret = 1
                    }
                }
            } ?: run {
                e.stopUsingItem()
                ret = 1
            }
            ret
        }
        override val ticksToExec: Int = 15
        override val ticksToTest: Int = 2
        override val radiusToAct: Float = 150.0f
        override val radiusToLook: Float = 150.0f
    }

    object Attach : Action() {
        override val scan: Input = { _, _ -> 9 }
        override val exec: Input = { e, p ->
            1
        }
        override val ticksToExec: Int = 1
        override val ticksToTest: Int = 1
        override val radiusToAct: Float = 0.0f
        override val radiusToLook: Float = 0.0f
    }

    enum class Type {
        PICK,
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
        MOVE,
        EAT,
        STORE,
        CHARGE,
        ATTACK,
        AIM,
        LOOK,
        OPEN,
        CLOSE,
        DEFEND,
        ATTACH,
        DETACH,
        YIELD, // placeholder action
    }

    companion object {
        private val map =
            mapOf(
                Type.PICK to Pick,
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
                Type.MOVE to Move,
                Type.EAT to Eat,
                Type.STORE to Store,
                Type.CHARGE to Charge,
                Type.ATTACK to Attack,
                Type.LOOK to Look,
                Type.OPEN to Open,
                Type.CLOSE to Close,
                Type.DEFEND to Defend,
                Type.AIM to Aim,
                Type.YIELD to Pick,
            )

        fun get(type: Type): Action = map[type] ?: Move

        // TODO: puts these on a general utils object
        fun Boolean.toByte(mult: Int): Byte = if (this) (1 * mult).toByte() else 0

        fun Boolean.toByte(): Byte = if (this) 1 else 0

        private val TILLABLE_BLOCKS = setOf(Blocks.DIRT_PATH, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT)
    }
}
