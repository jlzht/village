package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.ActionResult

fun interface VillagerLoadCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerLoadCallback::class.java,
        ) { listeners ->
            VillagerLoadCallback { entity ->
                for (listener in listeners) {
                    val result = listener.interact(entity)
                    if (result != ActionResult.PASS) {
                        return@VillagerLoadCallback result
                    }
                }
                return@VillagerLoadCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity): ActionResult
}
