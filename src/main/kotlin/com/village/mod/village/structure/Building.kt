package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
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
import net.minecraft.block.TrappedChestBlock
import net.minecraft.block.Waterloggable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LightType
import net.minecraft.world.World
import net.minecraft.world.poi.PointOfInterest
import net.minecraft.world.poi.PointOfInterestStorage
import java.util.ArrayDeque
import java.util.Queue
import java.util.stream.Collectors

class Building(val stype: StructureType, val lower: BlockPos, val upper: BlockPos, val start: BlockPos) : Structure() {
    override var type: StructureType = stype
    override var area: Region = Region(lower, upper)
    public val entrance = start
    override fun peelErrands(): List<Errand> {
        return errands.toList() // TODO: make errands order follow owners order
    }

    override fun genErrands(world: World) {
        if (errands.isEmpty()) {
            val data = Building.getStructureData(area.center(), area.lower, area.upper, world as ServerWorld)
            if (data != null) {
                LOGGER.info("CAPACITY TIME: {}", data.capacity)
                this.type = data.structure
                this.capacity = data.capacity
                this.errands = data.errands
            } else {
                // TODO: signal removal
            }
        }
    }

    public fun handleDoorInteraction(world: World): List<Errand> {
        val neighbours = listOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
        for (n in neighbours) {
            val k = entrance.add(n)
            LOGGER.info("I AM {} ", k)
            val blockState = world.getBlockState(k)
            if (blockState.getBlock() is DoorBlock) {
                if (!blockState.get(DoorBlock.OPEN)) {
                    return listOf(Errand(k, Action.OPEN), Errand(k, Action.CLOSE))
                }
                return listOf(Errand(k, Action.CLOSE))
            }
        }
        return emptyList()
    }

    companion object {
        val neighbours = setOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, -1, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
        fun getErrandByBlock(block: Block, pos: BlockPos): Errand {
            when (block) {
                is BedBlock -> return Errand(pos, Action.SLEEP)
                is SmokerBlock -> return Errand(pos, Action.BAKE)
                is TrappedChestBlock -> return Errand(pos, Action.STORE)
                is AbstractCauldronBlock -> return Errand(pos, Action.POUR)
                is BrewingStandBlock -> return Errand(pos, Action.BREW)
                is AnvilBlock -> return Errand(pos, Action.FORGE)
                is GrindstoneBlock -> return Errand(pos, Action.GRIND)
            }
            return Errand(pos, Action.MOVE)
        }

        fun getStructureData(pos: BlockPos, lower: BlockPos, upper: BlockPos, world: ServerWorld): StructureData? {
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
                .flatMap { ppos -> listOf(getErrandByBlock(world.getBlockState(ppos).getBlock(), ppos)) }

            errands.groupBy {
                when (it.component2()) {
                    Action.FORGE, Action.GRIND -> "FORGERY"
                    Action.POUR, Action.BREW -> "HUT"
                    Action.BAKE -> "KITCHEN"
                    Action.SLEEP -> "HOUSE"
                    else -> "NONE"
                }
            }.maxByOrNull { it.value.size }?.let { type ->
                val errand = type.value.toHashSet()
                when (type.key) {
                    "HUT" -> {
                        // TODO: put this in a function
                        val capacity = errand.size - Math.max(errand.count { it.component2() == Action.BREW }, errand.count { it.component2() == Action.POUR })
                        if (capacity > 0) {
                            return StructureData(StructureType.HUT, capacity, errand)
                        } else {
                            return null
                        }
                    }
                    "FORGERY" -> {
                        val capacity = errand.size - Math.max(errand.count { it.component2() == Action.FORGE }, errand.count { it.component2() == Action.GRIND })
                        if (capacity > 0) {
                            return StructureData(StructureType.FORGERY, capacity, errand.toHashSet())
                        } else {
                            return null
                        }
                    }
                    "HOUSE" -> {
                        return StructureData(StructureType.HOUSE, errand.size, errand)
                    }
                    "KITCHEN" -> {
                        return StructureData(StructureType.KITCHEN, errand.size, errand)
                    }
                    else -> null
                }
            }
            return null
        }

        // TODO: get rid of pair
        private fun detectBuildingEdges(world: World, startPos: BlockPos): Pair<Int, Region>? {
            val queue: Queue<BlockPos> = ArrayDeque()
            val visited = HashSet<BlockPos>()
            var edgeIterator: Int = 0
            var blockIterator: Int = 0
            var countLight: Int = 0
            var blockedNeighbours: Int
            var freeNeighbours: Int
            val edges = Region(startPos, startPos)
            queue.add(startPos)
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.contains(current)) {
                    blockIterator++
                    if (edgeIterator >= 32 || blockIterator >= 512) {
                        queue.clear()
                        return null
                    }
                    // freeNeighbours just helps making less expand calls
                    blockedNeighbours = 0
                    freeNeighbours = 0
                    for (neighbor in neighbours) {
                        val pos = BlockPos(current.x + neighbor.x, current.y + neighbor.y, current.z + neighbor.z)
                        if (!visited.contains(pos)) {
                            val blockState = world.getBlockState(pos)
                            if (this.isSuitableBlock(blockState, pos, visited, world)) {
                                countLight++
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
            return Pair(countLight, edges)
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
            this.detectBuildingEdges(player.world, pos)?.let { data ->
                val edges = data.second
                if (edges.area() >= data.first) {
                    player.sendMessage(Text.translatable("village.creation.building.fail.light"), true)
                    return null
                }
                edges.grow()
                if (!(edges.area() > 125)) {
                    player.sendMessage(Text.translatable("village.creation.building.fail.space"), true)
                    return null
                }
                this.getStructureData(edges.center(), edges.lower, edges.upper, player.world as ServerWorld)?.let { buildr ->
                    val building = Building(buildr.structure, edges.lower, edges.upper, pos)
                    building.capacity = buildr.capacity
                    building.errands = buildr.errands
                    player.sendMessage(Text.translatable("village.creation.building.success").append(Text.translatable(buildr.structure.name)), true)
                    return building
                }
                player.sendMessage(Text.translatable("village.creation.building.fail.empty"), true)
                return null
            }
            player.sendMessage(Text.translatable("village.creation.building.fail.bound"), true)
            return null
        }
    }
}
