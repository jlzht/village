package com.village.mod.world.event

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.ActionResult
import net.minecraft.world.World

fun interface HandBellUsageCallback {
    companion object {
        // @JvmField
        val EVENT = EventFactory.createArrayBacked(
            HandBellUsageCallback::class.java,
        ) { listeners ->
            HandBellUsageCallback { pos, world ->
                for (listener in listeners) {
                    val result = listener.interact(pos, world)
                    if (result != ActionResult.PASS) {
                        return@HandBellUsageCallback result
                    }
                }
                return@HandBellUsageCallback ActionResult.PASS
            }
        }
    }
    fun interact(entity: BlockPos, world: World): ActionResult
}
