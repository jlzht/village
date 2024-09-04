package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.ItemPredicate
import com.village.mod.village.structure.StructureType
import net.minecraft.item.Item
import net.minecraft.item.Items

abstract class Profession(
    val entity: CustomVillagerEntity,
) {
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
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.NONE
}

class Gatherer(
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.GATHERER
}

class Hunter(
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val structureInterest: StructureType = StructureType.NONE
    override val type = ProfessionType.HUNTER
}

class Farmer(
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val desiredItems: List<(Item) -> Boolean> =
        super.desiredItems + listOf(ItemPredicate.HOE, { item -> item == Items.BONE_MEAL }, ItemPredicate.PLANTABLE)
    override val structureInterest: StructureType = StructureType.FARM
    override val type = ProfessionType.FARMER
}

class Fisherman(
    entity: CustomVillagerEntity,
) : Profession(entity) {
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
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val type = ProfessionType.MERCHANT
    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf({ item -> item.defaultStack.isOf(Items.EMERALD) })
    override val structureInterest: StructureType = StructureType.MARKET
}

class Recruit(
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val desiredItems: List<(Item) -> Boolean> = super.desiredItems + listOf(ItemPredicate.SWORD)
    override val structureInterest: StructureType = StructureType.POND // barraks
    override val type = ProfessionType.RECRUIT
}

class Guard(
    entity: CustomVillagerEntity,
) : Profession(entity) {
    override val desiredItems: List<(Item) -> Boolean> =
        super.desiredItems + listOf(ItemPredicate.BOW, ItemPredicate.CROSSBOW, ItemPredicate.SWORD, ItemPredicate.ARMOR, ItemPredicate.SHIELD)
    override val structureInterest: StructureType = StructureType.POND // barraks
    override val type = ProfessionType.GUARD
}
