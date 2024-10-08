package com.settlement.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

fun interface HandBellUsageCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            HandBellUsageCallback::class.java,
        ) { listeners ->
            HandBellUsageCallback { player, pos ->
                for (listener in listeners) {
                    val result = listener.interact(player, pos)
                    if (result != ActionResult.PASS) {
                        return@HandBellUsageCallback result
                    }
                }
                return@HandBellUsageCallback ActionResult.PASS
            }
        }
    }
    fun interact(entity: PlayerEntity, pos: BlockPos): ActionResult
}
