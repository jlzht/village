package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.screen.Response
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
import net.minecraft.world.LightType
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.Queue

class Building(override var type: StructureType, lower: BlockPos, upper: BlockPos, capacity: Int) : Structure(capacity) {
    override val MAX_CAPACITY: Int = 3
    override val VOLUME_PER_RESIDENT: Int = 5
    override var area: Region = Region(lower, upper)
    override val settlers: MutableList<Int> = MutableList(MAX_CAPACITY) { -1 }

    override fun sortErrands() {}

    override fun getErrands(vid: Int): List<Errand> = emptyList()

    override fun updateErrands(world: World) {}

    companion object {
        val neighbours = setOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, -1, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))

        private fun getAction(pos: BlockPos, state: BlockState): Errand? {
            when (state.block) {
                is BedBlock -> return Errand(Action.Type.SLEEP, pos)
                is ChestBlock -> Errand(Action.Type.STORE, pos)
            }
            return null
        }

        fun assignStructure(region: Region, world: World): Pair<StructureType, List<Errand>>? {
            // just a placeholder, bleh
            region.iterateSurface().forEach { pos ->
                getAction(pos, world.getBlockState(pos))
            }
            // get the most representative blocks
            return null
        }

        // TODO: transform this in a iterator
        private fun getRegion(player: PlayerEntity, startPos: BlockPos): Region? {
            val queue: Queue<BlockPos> = ArrayDeque()
            val visited = HashSet<BlockPos>()
            var edgeIterator: Int = 0
            var blockIterator: Int = 0
            var lightCount: Int = 0
            val edges = Region(startPos, startPos)
            queue.add(startPos)

            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.contains(current)) {
                    blockIterator++
                    if (edgeIterator >= 32 || blockIterator >= 512) {
                        queue.clear()
                        Response.STRUCTURE_NOT_ENCLOSED.send(player)
                        return null
                    }
                    // freeNeighbours just helps making less expand calls
                    var blockedNeighbours = 0
                    var freeNeighbours = 0
                    for (neighbor in neighbours) {
                        val pos = BlockPos(current.x + neighbor.x, current.y + neighbor.y, current.z + neighbor.z)
                        if (!visited.contains(pos)) {
                            val blockState = player.world.getBlockState(pos)
                            if (this.isSuitableBlock(blockState, pos, player.world) && visited.contains(pos.up())) {
                                lightCount++
                                queue.add(pos)
                                freeNeighbours++
                            }
                        } else {
                            blockedNeighbours++
                            if (blockedNeighbours >= 3) break
                        }
                    }
                    if ((blockedNeighbours == 3 && freeNeighbours == 0) ||
                        (blockedNeighbours == 1 && freeNeighbours == 2) ||
                        (blockedNeighbours == 2 && freeNeighbours == 1)
                    ) {
                        edgeIterator++
                        edges.expand(current)
                    }
                }
                visited.add(current)
            }
            queue.clear()
            if (edges.area() >= lightCount) {
                Response.NOT_ENOUGHT_LIGHT.send(player)
                return null
            }
            edges.grow()
            if (edges.area() < 125) {
                Response.NOT_ENOUGHT_SPACE.send(player)
                return null
            }
            return edges
        }

        private fun isSuitableBlock(blockState: BlockState, blockPos: BlockPos, world: World): Boolean {
            return (
                (blockState.isOf(Blocks.AIR) && world.getLightLevel(LightType.BLOCK, blockPos) >= 9) ||
                    (blockState.getBlock() is HorizontalFacingBlock && !blockState.isIn(BlockTags.TRAPDOORS) && !blockState.isIn(BlockTags.FENCE_GATES)) ||
                    (blockState.getBlock() is Waterloggable)
                )
        }

        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val d = player.world.getBlockState(pos).get(DoorBlock.FACING)
            val r = player.blockPos.getSquaredDistance(pos.offset(d, 1).toCenterPos())
            val l = player.blockPos.getSquaredDistance(pos.offset(d.getOpposite(), 1).toCenterPos())
            val spos = if (r > l) { pos.offset(d, 1) } else { pos.offset(d.getOpposite()) }

            getRegion(player, spos)?.let { region ->
                assignStructure(region, player.world)?.let { (type, errands) ->
                    Response.NEW_STRUCTURE.send(player, type.name)
                    val building = Building(type, region.lower, region.upper, errands.count())
                    building.errands.addAll(errands)
                    LOGGER.info("Capacity: {}", errands.count())
                    LOGGER.info("Errands: {}", errands)
                    return building
                }
                Response.NOT_ENOUGHT_FURNITURE.send(player)
            }
            return null
        }
    }
}
