package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.ai.goal.Goal
import com.village.mod.village.structure.StructureType
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory

abstract class Profession() {
    abstract val type: ProfessionType
    init {}
    open fun addProfessionTasks(worker: CustomVillagerEntity) {}
    open fun castAction(worker: CustomVillagerEntity) {}
    abstract val structureInterest: StructureType //getWorkStructure(worker: CustomVillagerEntity) {}
}
