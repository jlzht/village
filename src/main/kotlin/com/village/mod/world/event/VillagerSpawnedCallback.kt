package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.ActionResult

fun interface VillagerSpawnedCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerSpawnedCallback::class.java,
        ) { listeners ->
            VillagerSpawnedCallback { entity ->
                for (listener in listeners) {
                    val result = listener.interact(entity)
                    if (result != ActionResult.PASS) {
                        return@VillagerSpawnedCallback result
                    }
                }
                return@VillagerSpawnedCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity): ActionResult
}
