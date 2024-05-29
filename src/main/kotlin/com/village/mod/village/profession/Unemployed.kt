package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.minecraft.item.Items
import net.minecraft.item.Item

class Unemployed() : Profession() {
    override val type = ProfessionType.NONE
    override val desiredItems: (Item) -> Boolean = { item -> item.defaultStack.isOf(Items.EMERALD) }
    init {
    }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
    override val structureInterest: StructureType = StructureType.NONE
}
