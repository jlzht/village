package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.screen.Response
import com.village.mod.util.BlockIterator
import com.village.mod.util.Region
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.Waterloggable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.Queue

class Building(
    override var type: StructureType,
    lower: BlockPos,
    upper: BlockPos,
    capacity: Int,
) : Structure(capacity) {
    override val MAX_CAPACITY: Int = 3
    override val VOLUME_PER_RESIDENT: Int = 5
    override var region: Region = Region(lower, upper)
    override val settlers: MutableList<Int> = MutableList(MAX_CAPACITY) { -1 }

    override fun sortErrands() {}

    override fun getErrands(vid: Int): List<Errand> = emptyList()

    override fun updateErrands(world: World) {}

    companion object {

        private fun getAction(
            pos: BlockPos,
            state: BlockState,
        ): Errand? {
            when (state.block) {
                is BedBlock -> return Errand(Action.Type.SLEEP, pos)
                is ChestBlock -> Errand(Action.Type.STORE, pos)
            }
            return null
        }

        fun assignStructure(
            region: Region,
            world: World,
        ): Pair<StructureType, List<Errand>>? {
            // just a placeholder, bleh
            BlockIterator.CUBOID(region.lower, region.upper).forEach { pos ->
                getAction(pos, world.getBlockState(pos))
            }
            // get the most representative blocks
            return null
        }

        // TODO: transform this into a iterator
        private fun getRegion(
            player: PlayerEntity,
            startPos: BlockPos,
        ): Region? {
            val queue: Queue<BlockPos> = ArrayDeque()
            val visited = HashSet<BlockPos>()
            var edgeIterator: Int = 0 // total edges found
            var iterations: Int = 0
            var lightCount: Int = 0
            val edges = Region(startPos, startPos)
            queue.add(startPos)
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.contains(current)) {
                    iterations++
                    if (edgeIterator >= 32 || iterations >= 512) {
                        queue.clear()
                        Response.STRUCTURE_NOT_ENCLOSED.send(player)
                        return null
                    }
                    var blockedCount = 0 // counts neighbours already added
                    var freeCount = 0 // counts neighbours added
                    BlockIterator.TOUCHING(current).forEach { pos ->
                        if (!visited.contains(pos)) {
                            val blockState = player.world.getBlockState(pos)
                            if (blockState.isOf(Blocks.AIR) ||
                                (
                                    blockState.getBlock() is HorizontalFacingBlock &&
                                        !blockState.isIn(BlockTags.TRAPDOORS) &&
                                        !blockState.isIn(BlockTags.FENCE_GATES)
                                ) ||
                                (blockState.getBlock() is Waterloggable) &&
                                visited.contains(pos.up())
                            ) {
                                queue.add(pos)
                                lightCount++
                                freeCount++
                            }
                        } else {
                            if (blockedCount <= 3) {
                                blockedCount++
                            }
                        }
                    }
                    if ((blockedCount == 3 && freeCount == 0) ||
                        (blockedCount == 1 && freeCount == 2) ||
                        (blockedCount == 2 && freeCount == 1)
                    ) {
                        edgeIterator++
                        LOGGER.info("blocked: {} flee: {} current: {}", blockedCount, freeCount, current)
                        edges.append(current)
                    }
                }
                visited.add(current)
            }
            queue.clear()
            // ignore this the rest
            // put lights check of get errands
            if (edges.volume() >= lightCount) {
                Response.NOT_ENOUGHT_LIGHT.send(player)
                return null
            }
            LOGGER.info("> {}", lightCount)
            edges.grow()
            // use VOLUME_PER_RESIDENT field
            if (edges.volume() < 125) {
                Response.NOT_ENOUGHT_SPACE.send(player)
                return null
            }
            LOGGER.info("> {}", edges.volume())
            return edges
        }

        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val d = player.world.getBlockState(pos).get(DoorBlock.FACING)
            val r = player.blockPos.getSquaredDistance(pos.offset(d, 1).toCenterPos())
            val l = player.blockPos.getSquaredDistance(pos.offset(d.getOpposite(), 1).toCenterPos())
            val spos =
                if (r > l) {
                    pos.offset(d, 1)
                } else {
                    pos.offset(d.getOpposite())
                }

            getRegion(player, spos) // ?.let { region ->

            // assignStructure(region, player.world)?.let { (type, errands) ->
            //    Response.NEW_STRUCTURE.send(player, type.name)
            //    val building = Building(type, region.lower, region.upper, errands.count())
            //    building.errands.addAll(errands)
            //    LOGGER.info("Capacity: {}", errands.count())
            //    LOGGER.info("Errands: {}", errands)
            //    return building
            // }
            // Response.NOT_ENOUGHT_FURNITURE.send(player)
            // }
            return null
        }
    }
}
