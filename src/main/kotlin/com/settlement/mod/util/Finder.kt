package com.settlement.mod.util

import com.settlement.mod.action.Action
import com.settlement.mod.action.Errand
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.NoPenaltyTargeting
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object Finder {
    fun findWaterBlock(
        pos: BlockPos,
        world: World,
    ): BlockPos? =
        BlockPos
            .findClosest(pos, 8, 5, { wpos ->
                BlockIterator.NEIGHBOURS(wpos).all { world.getBlockState(it).isOf(Blocks.WATER) && world.getBlockState(wpos.up()).isOf(Blocks.AIR) }
            })
            .orElse(null)

    fun findSurfaceBlock(
        pos: BlockPos,
        world: World,
    ): Errand? =
        BlockPos
            .findClosest(pos, 4, 4, { spos ->
                world.getBlockState(spos).isSolid && world.getBlockState(spos.up()).isAir && world.getBlockState(spos.up(2)).isAir
            })
            .orElse(null)
            ?.let {
                return Errand(Action.Type.MOVE, it)
            }

    fun findFleeBlock(
        entity: AbstractVillagerEntity,
        target: LivingEntity,
    ): Errand? {
        NoPenaltyTargeting.findFrom(entity, 12, 4, target.getPos())?.let { t ->
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
            return Errand(Action.Type.OPEN, doorPos)
        }
        return null
    }
}
