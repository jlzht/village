package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.VigerEntity
import net.minecraft.block.Block
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.math.BlockPos

class CheckNearbyBlocksGoal(private val entity: VigerEntity) : Goal() {
    override fun canStart(): Boolean {
        if (entity.getWorld().getTime() > entity.checkupTime + 50L) {
            entity.checkupTime = entity.getWorld().getTime()
            return true
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        return false
    }

    override fun start() {
        val pos = entity.getBlockPos()
        for (x in -2..2) {
            for (y in -2..2) {
                for (z in -2..2) {
                    val ht: BlockPos = BlockPos(pos.x + x, pos.y + y, pos.z + z)
                    val block: Block = entity.world.getBlockState(ht).block
                    LOGGER.info("[{},{},{}] - BLOCK: {} ", ht.x, ht.y, ht.z, block.name.string)
                }
            }
        }
    }

    override fun stop() {
    }
}
