package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity

class Merchant() : Profession() {
    override val type = ProfessionType.MERCHANT
    init {
    }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
}
