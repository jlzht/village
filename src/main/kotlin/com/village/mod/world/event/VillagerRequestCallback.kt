package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.ActionResult

fun interface VillagerRequestCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerRequestCallback::class.java,
        ) { listeners ->
            VillagerRequestCallback { entity, villageID ->
                for (listener in listeners) {
                    val result = listener.interact(entity, villageID)
                    if (result != ActionResult.PASS) {
                        return@VillagerRequestCallback result
                    }
                }
                return@VillagerRequestCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity, villageID: Int): ActionResult
}
