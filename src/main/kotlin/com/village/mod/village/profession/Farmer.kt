package com.village.mod.village.profession

//import com.village.mod.entity.ai.goal.TillingGoal
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.village.structure.StructureType
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER

class Farmer() : Profession() {
    override val type = ProfessionType.FARMER
    init {}
    // blockInteraction
    override fun addProfessionTasks(worker: CustomVillagerEntity) {
        //worker.appendGoal(2, TillingGoal(worker))
    }
    override val structureInterest: StructureType = StructureType.FARM
}
