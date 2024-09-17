package com.settlement.mod.profession

import com.settlement.mod.action.Action
import net.minecraft.util.Hand
import com.settlement.mod.entity.projectile.SimpleFishingBobberEntity
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.item.ItemPredicate
import com.settlement.mod.structure.StructureType
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.loot.LootTables
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.server.world.ServerWorld

abstract class Profession {
    abstract val type: ProfessionType
    open val desiredItems: List<(Item) -> Boolean> = listOf(ItemPredicate.EDIBLE)
    abstract val structureInterest: StructureType

    companion object {
        fun get(
            entity: AbstractVillagerEntity,
            type: ProfessionType,
        ): Profession =
            when (type) {
                ProfessionType.GATHERER -> Gatherer(entity)
                ProfessionType.HUNTER -> Hunter(entity)
                ProfessionType.FARMER -> Farmer(entity)
                ProfessionType.FISHERMAN -> Fisherman(entity)
                ProfessionType.MERCHANT -> Merchant(entity)
                ProfessionType.GUARD -> Guard(entity)
                ProfessionType.RECRUIT -> Recruit(entity)
                else -> Unemployed(entity)
            }
    }
}

class Unemployed(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.NONE
}

class Gatherer(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.GATHERER
}

class Hunter(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.HUNTER
}

class Farmer(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val desiredItems: List<(Item) -> Boolean> =
        super.desiredItems + listOf(ItemPredicate.HOE, { item -> item == Items.BONE_MEAL }, ItemPredicate.PLANTABLE)
    override val structureInterest: StructureType = StructureType.FARM
    override val type = ProfessionType.FARMER
}

class Fisherman(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf(ItemPredicate.FISHING_ROD)
    override val structureInterest: StructureType = StructureType.POND
    override val type = ProfessionType.FISHERMAN

    private var fishHook: SimpleFishingBobberEntity? = null

    fun getFishHook(): SimpleFishingBobberEntity? = fishHook

    fun setCaughtFish() {
        getFishHook()?.let { hook ->
            val world = entity.world
            val stack = entity.getStackInHand(Hand.MAIN_HAND)
            val lootContextParameterSet =
                LootContextParameterSet
                    .Builder(world as ServerWorld)
                    .add(LootContextParameters.ORIGIN, hook.pos)
                    .add(LootContextParameters.TOOL, stack)
                    .add(LootContextParameters.THIS_ENTITY, hook)
                    // .luck(luckOfTheSeaLevel.toFloat() + entity.luck.toFloat())
                    .build(LootContextTypes.FISHING)
            val lootTable = world.server.lootManager.getLootTable(LootTables.FISHING_GAMEPLAY)
            val lootList = lootTable.generateLoot(lootContextParameterSet)

            for (itemStack in lootList) {
                val itemEntity = ItemEntity(world, hook.x, hook.y, hook.z, itemStack)
                val d = entity.x - hook.x
                val e = entity.y - hook.y
                val f = entity.z - hook.z
                val g = 0.1
                itemEntity.setVelocity(d * 0.1, e * 0.1 + Math.sqrt(Math.sqrt(d * d + e * e + f * f)) * 0.08, f * 0.1)
                world.spawnEntity(itemEntity)
            }
        }
    }

    fun setFishHook(hook: SimpleFishingBobberEntity?) {
        this.fishHook = hook
    }
}

class Merchant(
    val entity: AbstractVillagerEntity,
) : Profession() {
    override val type = ProfessionType.MERCHANT
    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf({ item -> item.defaultStack.isOf(Items.EMERALD) })
    override val structureInterest: StructureType = StructureType.MARKET
}

interface Combatant {
    abstract var cache: Map<Action.Type, Boolean>

    abstract fun generateCache(): Map<Action.Type, Boolean>

    abstract fun updateCache()
}

class Recruit(
    val entity: AbstractVillagerEntity,
) : Profession(),
    Combatant {
    init {
        entity.inventory.setUpdater({ updateCache() })
    }

    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf(ItemPredicate.SWORD, ItemPredicate.ARMOR)
    override val structureInterest: StructureType = StructureType.POND // barraks
    override val type = ProfessionType.RECRUIT

    override fun updateCache() {
        cache = generateCache()
    }

    override fun generateCache(): Map<Action.Type, Boolean> =
        mapOf(
            Action.Type.ATTACK to entity.inventory.hasItem(ItemPredicate.SWORD),
        )

    override var cache: Map<Action.Type, Boolean> = emptyMap()
}

class Guard(
    val entity: AbstractVillagerEntity,
) : Profession(),
    Combatant {
    init {
        entity.inventory.setUpdater({ updateCache() })
    }

    override val desiredItems: List<(Item) -> Boolean> =
        super.desiredItems +
            listOf(ItemPredicate.BOW, ItemPredicate.CROSSBOW, ItemPredicate.SWORD, ItemPredicate.ARMOR, ItemPredicate.SHIELD)
    override val structureInterest: StructureType = StructureType.POND // barraks
    override val type = ProfessionType.GUARD

    override fun updateCache() {
        cache = generateCache()
    }

    override fun generateCache(): Map<Action.Type, Boolean> =
        mapOf(
            Action.Type.CHARGE to entity.inventory.hasItem(ItemPredicate.CROSSBOW),
            Action.Type.AIM to entity.inventory.hasItem(ItemPredicate.BOW),
            Action.Type.ATTACK to entity.inventory.hasItem(ItemPredicate.SWORD),
            Action.Type.DEFEND to entity.inventory.hasItem(ItemPredicate.SHIELD),
        )

    override var cache: Map<Action.Type, Boolean> = emptyMap()
}
