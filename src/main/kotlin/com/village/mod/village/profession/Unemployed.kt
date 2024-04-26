package com.village.mod.village.profession

import com.village.mod.entity.village.CustomVillagerEntity

class Unemployed() : Profession() {
    override val type = ProfessionType.NONE
    init {
    }
    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
}
