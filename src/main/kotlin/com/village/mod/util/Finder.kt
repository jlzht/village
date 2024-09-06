package com.village.mod.util

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.block.DoorBlock
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.NoPenaltyTargeting
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object Finder {
    fun findFleeBlock(
        entity: CustomVillagerEntity,
        target: LivingEntity,
    ): Errand? {
        NoPenaltyTargeting.findFrom(entity, 24, 4, target.getPos())?.let { t ->
            return Errand(Action.Type.FLEE, BlockPos(t.x.toInt(), t.y.toInt(), t.z.toInt()))
        }
        return null
    }

    fun findDoorBlock(
        world: World,
        path: Path,
    ): Errand? {
        for (i in 0 until minOf(path.currentNodeIndex + 2, path.length)) {
            val pathNode = path.getNode(i)
            val doorPos = BlockPos(pathNode.x, pathNode.y + 1, pathNode.z)
            val doorValid = DoorBlock.canOpenByHand(world, doorPos)
            if (!doorValid) continue
            LOGGER.info("THERE IS A DOOR IN THE WAY")
            return Errand(Action.Type.OPEN, doorPos)
        }
        return null
    }
}