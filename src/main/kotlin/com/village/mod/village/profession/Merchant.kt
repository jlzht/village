package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType

class Merchant() : Profession() {
    override val type = ProfessionType.MERCHANT
    init {
    }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
    override val structureInterest: StructureType = StructureType.MARKET
}
