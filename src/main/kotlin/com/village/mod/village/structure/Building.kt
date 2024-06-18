package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.screen.Response
import com.village.mod.world.graph.Node
import net.minecraft.block.AbstractCauldronBlock
import net.minecraft.block.AnvilBlock
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.BrewingStandBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.GrindstoneBlock
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.SmokerBlock
import net.minecraft.block.Waterloggable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LightType
import net.minecraft.world.World
import net.minecraft.world.poi.PointOfInterest
import net.minecraft.world.poi.PointOfInterestStorage
import java.util.ArrayDeque
import java.util.Queue
import java.util.stream.Collectors

class Building(override var type: StructureType, lower: BlockPos, upper: BlockPos) : Structure() {
    override var area: Region = Region(lower, upper)

    companion object {
        val neighbours = setOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, -1, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
        fun getNode(block: Block, pos: BlockPos): Node<Int> {
            LOGGER.info("---- {}", pos)
            when (block) {
                is BedBlock -> return Node(9, pos, 0.5f, emptyList())
                is SmokerBlock -> return Node(1, pos, 0.5f, emptyList())
                is AnvilBlock -> return Node(6, pos, 0.5f, emptyList())
                is GrindstoneBlock -> return Node(7, pos, 0.5f, emptyList())
                is AbstractCauldronBlock -> return Node(10, pos, 0.5f, emptyList())
                is BrewingStandBlock -> return Node(11, pos, 0.5f, emptyList())
            }
            return Node(0, pos, 0.5f, emptyList())
        }

        fun assignStructure(pos: BlockPos, lower: BlockPos, upper: BlockPos, world: ServerWorld, list: MutableList<Node<Int>>): StructureType? {
            val region = Region(lower, upper)
            val pointOfInterestStorage = world.getPointOfInterestStorage()
            val errands = pointOfInterestStorage.getInSquare(
                { _ -> true },
                pos,
                8,
                PointOfInterestStorage.OccupationStatus.ANY,
            ).filter { poi -> region.contains(poi.getPos()) }
                .map(PointOfInterest::getPos)
                .collect(Collectors.toSet())
                .flatMap { ppos -> listOf(getNode(world.getBlockState(ppos).getBlock(), ppos)) }
            LOGGER.info("SSS{}", errands)
            errands.groupBy {
                when (it.cid) {
                    in 6..7 -> "FORGERY"
                    in 10..11 -> "HUT"
                    1 -> "KITCHEN"
                    9 -> "HOUSE"
                    else -> "NONE"
                }
            }.maxByOrNull { it.value.size }?.let { type ->
                val node = type.value
                LOGGER.info(" {} -- {}", type.key, type.value)
                when (type.key) {
                    "FORGERY" -> {
                        list.addAll(node)
                        return StructureType.FORGERY
                    }
                    "HOUSE" -> {
                        list.addAll(node)
                        return StructureType.HOUSE
                    }
                    "KITCHEN" -> {
                        list.addAll(node)
                        return StructureType.KITCHEN
                    }
                    "HUT" -> {
                        list.addAll(node)
                        return StructureType.HUT
                    }
                    else -> null
                }
            }
            return null
        }

        // TODO: transform this in a iterator
        private fun detectBuildingEdges(player: PlayerEntity, startPos: BlockPos): Region? {
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
                            if (this.isSuitableBlock(blockState, pos, visited, player.world)) {
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

        private fun isSuitableBlock(blockState: BlockState, blockPos: BlockPos, visited: HashSet<BlockPos>, world: World): Boolean {
            if ((blockState.isOf(Blocks.AIR) && world.getLightLevel(LightType.BLOCK, blockPos) >= 9) ||
                (blockState.getBlock() is HorizontalFacingBlock && !blockState.isIn(BlockTags.TRAPDOORS) && !blockState.isIn(BlockTags.FENCE_GATES)) ||
                (blockState.getBlock() is Waterloggable && visited.contains(blockPos.up()))
            ) {
                return true
            }
            return false
        }

        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val d = player.world.getBlockState(pos).get(DoorBlock.FACING)
            val r = player.blockPos.getSquaredDistance(pos.offset(d, 1).toCenterPos())
            val l = player.blockPos.getSquaredDistance(pos.offset(d.getOpposite(), 1).toCenterPos())
            val spos = if (r > l) { pos.offset(d, 1) } else { pos.offset(d.getOpposite()) }
            val nodes = mutableListOf<Node<Int>>()
            nodes.add(Node(0, pos.offset(d, 1), 0.5f, emptyList())) // inside
            nodes.add(Node(5, pos.offset(d.getOpposite(), 1), 0.5f, emptyList())) // outside
            this.detectBuildingEdges(player, spos)?.let { edges ->
                LOGGER.info("I GOT HERE@")
                this.assignStructure(edges.center(), edges.lower, edges.upper, player.world as ServerWorld, nodes)?.let { type ->
                    LOGGER.info("I GOT THERE@")
                    // abstract this
                    Response.NEW_STRUCTURE.send(player, type.name)
                    val building = Building(type, edges.lower, edges.upper)
                    building.graph.loadNode(0, nodes[1])
                    building.graph.loadNode(1, nodes[0])
                    building.graph.addEdge(0, 1)
                    nodes.drop(2).forEachIndexed { index, node ->
                        val nodeIndex = index + 2
                        building.graph.loadNode(nodeIndex, node)
                        building.graph.addEdge(1, nodeIndex)
                    }
                    return building
                }
                Response.NOT_ENOUGHT_FURNITURE.send(player)
            }
            return null
        }
    }
}
