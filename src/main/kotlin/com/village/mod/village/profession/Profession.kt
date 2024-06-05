package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.minecraft.item.Item

abstract class Profession(val villager: CustomVillagerEntity) {
    abstract val type: ProfessionType
    abstract val desiredItems: (Item) -> Boolean
    init {}
    open fun doWork() {}
    open fun canWork(): Boolean { return false }
    abstract val structureInterest: StructureType
}
