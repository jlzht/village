package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.ActionResult
import com.village.mod.village.structure.Structure
import com.village.mod.village.structure.StructureType

fun interface VillagerRequestCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerRequestCallback::class.java,
        ) { listeners ->
            VillagerRequestCallback { entity, type ->
                for (listener in listeners) {
                    val result = listener.interact(entity, type)
                    if (result != ActionResult.PASS) {
                        return@VillagerRequestCallback result
                    }
                }
                return@VillagerRequestCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity, type: StructureType): ActionResult
}
