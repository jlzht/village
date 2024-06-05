package com.village.mod.world.event

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.util.ActionResult

fun interface VillagerRequestCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerRequestCallback::class.java,
        ) { listeners ->
            VillagerRequestCallback { entity, keys ->
                for (listener in listeners) {
                    val result = listener.interact(entity, keys)
                    if (result != ActionResult.PASS) {
                        return@VillagerRequestCallback result
                    }
                }
                return@VillagerRequestCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity, keys: IntArray): ActionResult
}
