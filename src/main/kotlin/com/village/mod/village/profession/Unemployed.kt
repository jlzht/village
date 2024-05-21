package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType

class Unemployed() : Profession() {
    override val type = ProfessionType.NONE
    init {
    }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
    override val structureInterest: StructureType = StructureType.NONE
}
