package com.village.mod.village.profession

import com.village.mod.entity.ai.goal.HoeingGoal
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER

class Farmer() : Profession() {
    override val type = ProfessionType.FARMER
    init {}
    // blockInteraction
    override fun addProfessionTasks(worker: CustomVillagerEntity) {
        worker.appendGoal(2, HoeingGoal(worker))
    }
}
