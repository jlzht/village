package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.util.ActionResult

fun interface VillagerKilledCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            VillagerKilledCallback::class.java,
        ) { listeners ->
            VillagerKilledCallback { entity ->
                for (listener in listeners) {
                    val result = listener.interact(entity)
                    if (result != ActionResult.PASS) {
                        return@VillagerKilledCallback result
                    }
                }
                return@VillagerKilledCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: CustomVillagerEntity): ActionResult
}

