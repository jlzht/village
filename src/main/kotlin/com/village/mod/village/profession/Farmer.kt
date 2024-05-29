package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.village.structure.StructureType
import com.google.common.collect.ImmutableSet
import net.minecraft.item.Items
import net.minecraft.item.Item
import net.minecraft.item.HoeItem
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER

class Farmer() : Profession() {
    override val type = ProfessionType.FARMER
    override val desiredItems: (Item) -> Boolean = { item -> item is HoeItem || item.defaultStack.isIn(ItemTags.VILLAGER_PLANTABLE_SEEDS) }
    init {

    }
    // blockInteraction
    override fun addProfessionTasks(worker: CustomVillagerEntity) {
        //worker.appendGoal(2, TillingGoal(worker))
    }
    override val structureInterest: StructureType = StructureType.FARM
}
