package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.minecraft.item.Item

abstract class Profession() {
    abstract val type: ProfessionType
    abstract val desiredItems: (Item) -> Boolean
    init {}
    open fun addProfessionTasks(worker: CustomVillagerEntity) {}
    open fun doWork(worker: CustomVillagerEntity) {}
    open fun canWork(worker: CustomVillagerEntity): Boolean { return true }
    abstract val structureInterest: StructureType
}
