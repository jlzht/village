package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.ActionResult
import net.minecraft.world.World

fun interface HandBellUsageCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            HandBellUsageCallback::class.java,
        ) { listeners ->
            HandBellUsageCallback { player, pos, world ->
                for (listener in listeners) {
                    val result = listener.interact(player, pos, world)
                    if (result != ActionResult.PASS) {
                        return@HandBellUsageCallback result
                    }
                }
                return@HandBellUsageCallback ActionResult.PASS
            }
        }
    }
    fun interact(entity: PlayerEntity,pos: BlockPos, world: World): ActionResult
}
