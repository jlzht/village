package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

fun interface VillageInteractionCallback {
    companion object {
        @JvmField // needed for calling from mixins
        val EVENT = EventFactory.createArrayBacked(
            VillageInteractionCallback::class.java,
        ) { listeners ->
            VillageInteractionCallback { entity, block, name ->
                for (listener in listeners) {
                    val result = listener.interact(entity, block, name)
                    if (result != ActionResult.PASS) {
                        return@VillageInteractionCallback result
                    }
                }
                return@VillageInteractionCallback ActionResult.PASS
            }
        }
    }

    fun interact(entity: PlayerEntity, block: BlockPos, name: String): ActionResult
}
