package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
import com.village.mod.village.VillageUtils
import net.minecraft.block.AbstractCauldronBlock
import net.minecraft.block.AnvilBlock
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.BrewingStandBlock
import net.minecraft.block.GrindstoneBlock
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.SmokerBlock
import net.minecraft.block.TrappedChestBlock
import net.minecraft.block.Waterloggable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LightType
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.Queue


class Building(val stype: StructureType, val lower: BlockPos, val upper: BlockPos, val start: BlockPos) : Structure() {
    override var type: StructureType = stype
    override var area: Area = Region(lower, upper)
    public val entrance = start
    override fun peelErrands(): List<Errand> {
        return errands.toList()
    }

    override fun genErrands(world: World) {
        if (errands.isEmpty()) {
            val data = VillageUtils.getStructureData((area as Region).center(), (area as Region).lower, (area as Region).upper, world as ServerWorld)
            if (data != null) {
                LOGGER.info("CAPACITY TIME: {}", data.capacity)
                this.type = data.structure
                this.capacity = data.capacity
                this.errands = data.errands
            } else {
                LOGGER.info("NOT ENOUGH FURNITURE TO INFER")
            }
        }
    }

    companion object {
        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val world = player.world
            val queue: Queue<BlockPos> = ArrayDeque()
            val visited = HashSet<BlockPos>()
            var edgeIterator: Int = 0
            var blockIterator: Int = 0
            var countLight: Int = 0
            var blockedNeighbours: Int = 0
            var freeNeighbours: Int = 0
            val neighbours = listOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, -1, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
            val edges = Region(pos, pos)
            queue.add(pos)
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.contains(current)) {
                    blockIterator++
                    if (edgeIterator >= 32 || blockIterator >= 512) {
                        player.sendMessage(Text.translatable("village.creation.building.fail.bound"), true)
                        queue.clear()
                        return null
                    }
                    // TODO: optimize this
                    for (neighbor in neighbours) {
                        val blockr = BlockPos(current.x + neighbor.x, current.y + neighbor.y, current.z + neighbor.z)
                        if (!visited.contains(blockr)) {
                            val bk = world.getBlockState(blockr)
                            if (bk.isOf(Blocks.AIR)) {
                                if (world.getLightLevel(LightType.BLOCK, blockr) >= 9) {
                                    countLight++
                                }
                                queue.add(blockr)
                                freeNeighbours++
                            } else if (bk.getBlock() is HorizontalFacingBlock && !bk.isIn(BlockTags.TRAPDOORS) && !bk.isIn(BlockTags.FENCE_GATES)) {
                                countLight++
                                queue.add(blockr)
                                freeNeighbours++
                            } else if (bk.getBlock() is Waterloggable && visited.contains(blockr.up())) {
                                countLight++
                                queue.add(blockr)
                                freeNeighbours++
                            }
                        } else {
                            blockedNeighbours++
                        }
                    }

                    if ((freeNeighbours == 0 && blockedNeighbours == 3) ||
                        (blockedNeighbours == 1 && freeNeighbours == 2) ||
                        (blockedNeighbours == 2 && freeNeighbours == 1)
                    ) {
                        edgeIterator++
                        edges.expand(current)
                    }
                    blockedNeighbours = 0
                    freeNeighbours = 0
                }
                visited.add(current)
            }
            queue.clear()
            if (edges.area() >= countLight) {
                player.sendMessage(Text.translatable("village.creation.building.fail.light"), true)
                return null
            }
            edges.grow()
            val area = edges.area()
            if (!(area > 125)) {
                player.sendMessage(Text.translatable("village.creation.building.fail.space"), true)
                return null
            }
            LOGGER.info("| AREA: {} - LIGHT: {} - LOWER: {}, UPPER: {}", area, countLight, edges.lower, edges.upper)
            val data = VillageUtils.getStructureData(edges.center(), edges.lower, edges.upper, world as ServerWorld)
            if (data == null) {
                player.sendMessage(Text.translatable("village.creation.building.fail.empty"), true)
                return null
            }
            val building = Building(data.structure, edges.lower, edges.upper, pos)
            building.capacity = data.capacity
            building.errands = data.errands
            player.sendMessage(Text.translatable("village.creation.building.success").append(Text.translatable(data.structure.name)), true)
            return building
        }
    }
}
