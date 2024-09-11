package com.village.mod.village.profession

import com.village.mod.action.Action
import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.ItemPredicate
import com.village.mod.village.structure.StructureType
import net.minecraft.item.Item
import net.minecraft.item.Items

abstract class Profession {
    abstract val type: ProfessionType
    open val desiredItems: List<(Item) -> Boolean> = listOf(ItemPredicate.EDIBLE)
    abstract val structureInterest: StructureType

    companion object {
        fun get(
            entity: CustomVillagerEntity,
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
    val entity: CustomVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.NONE
}

class Gatherer(
    val entity: CustomVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.GATHERER
}

class Hunter(
    val entity: CustomVillagerEntity,
) : Profession() {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.HUNTER
}

class Farmer(
    val entity: CustomVillagerEntity,
) : Profession() {
    override val desiredItems: List<(Item) -> Boolean> =
        super.desiredItems + listOf(ItemPredicate.HOE, { item -> item == Items.BONE_MEAL }, ItemPredicate.PLANTABLE)
    override val structureInterest: StructureType = StructureType.FARM
    override val type = ProfessionType.FARMER
}

class Fisherman(
    val entity: CustomVillagerEntity,
) : Profession() {
    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf(ItemPredicate.FISHING_ROD)
    override val structureInterest: StructureType = StructureType.POND
    override val type = ProfessionType.FISHERMAN

    private var fishHook: SimpleFishingBobberEntity? = null

    fun getFishHook(): SimpleFishingBobberEntity? = fishHook

    fun setFishHook(hook: SimpleFishingBobberEntity?) {
        this.fishHook = hook
    }
}

class Merchant(
    val entity: CustomVillagerEntity,
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
    val entity: CustomVillagerEntity,
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
    val entity: CustomVillagerEntity,
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
