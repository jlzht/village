package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.screen.Response
import com.village.mod.util.BlockIterator
import com.village.mod.util.Region
import net.minecraft.block.Blocks
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class Farm(
    val lower: BlockPos,
    val upper: BlockPos,
    capacity: Int,
) : Structure(capacity) {
    override val MAX_CAPACITY: Int = 3
    override val VOLUME_PER_RESIDENT: Int = 5
    override var type: StructureType = StructureType.FARM
    override var region: Region = Region(lower, upper)
    override val settlers: MutableList<Int> = MutableList(MAX_CAPACITY) { -1 }

    override fun updateErrands(world: World) {
        // errands.clear()
        BlockIterator.CUBOID(region.lower, region.upper).forEach { pos ->
            getFarmAction(pos, world)?.let { action ->
                LOGGER.info("| Action: {}, Pos: {}", action, pos)
                if (!region.contains(pos)) {
                    region.append(pos)
                    LOGGER.info("New volume: {}", region.volume())
                }
                val apos = if (action == Action.Type.HARVEST) pos.up() else pos
                val e = Errand(action, apos)
                if (!errands.contains(e)) {
                    LOGGER.info("Already have!")
                }
                errands.add(e)
            }
        }
        // sortErrands()
    }

    override fun sortErrands() {}

    override fun getErrands(vid: Int): List<Errand>? {
        if (!hasErrands()) return null
        val amount = errands.size / capacity
        val taken = errands.take(amount)
        errands.removeAll(taken)
        return taken
    }

    private fun getFarmAction(
        pos: BlockPos,
        world: World,
    ): Action.Type? {
        val block = world.getBlockState(pos)
        LOGGER.info("--{}", block)
        if (block.isOf(Blocks.FARMLAND) && block.get(FarmlandBlock.MOISTURE) >= 5) {
            if (world.getBlockState(pos.up()).isOf(Blocks.AIR)) {
                return Action.Type.PLANT
            } else {
                val up = world.getBlockState(pos.up())
                val upBlock = up.getBlock()
                if (upBlock is CropBlock) {
                    if (upBlock.isMature(up)) {
                        return Action.Type.HARVEST
                    } else {
                        return Action.Type.POWDER
                    }
                }
            }
        }
        if (block.isOf(Blocks.GRASS_BLOCK) || block.isOf(Blocks.DIRT)) {
            if (world.getBlockState(pos.up()).isOf(Blocks.AIR)) {
                val north = world.getBlockState(pos.north())
                val south = world.getBlockState(pos.south())
                val east = world.getBlockState(pos.east())
                val west = world.getBlockState(pos.west())
                if (
                    (
                        (north.isOf(Blocks.FARMLAND) && north.get(FarmlandBlock.MOISTURE) >= 5) ||
                            (south.isOf(Blocks.FARMLAND) && south.get(FarmlandBlock.MOISTURE) >= 5)
                    ) &&
                    (
                        (west.isOf(Blocks.FARMLAND) && west.get(FarmlandBlock.MOISTURE) >= 5) ||
                            (east.isOf(Blocks.FARMLAND) && east.get(FarmlandBlock.MOISTURE) >= 5)
                    )
                ) {
                    val northwest = world.getBlockState(pos.north().west())
                    val northeast = world.getBlockState(pos.north().east())
                    val southeast = world.getBlockState(pos.south().east())
                    val southwest = world.getBlockState(pos.south().west())
                    if (
                        northeast.isOf(Blocks.FARMLAND) ||
                        northwest.isOf(Blocks.FARMLAND) ||
                        southeast.isOf(Blocks.FARMLAND) ||
                        southwest.isOf(Blocks.FARMLAND)
                    ) {
                        return Action.Type.TILL
                    }
                }
            }
        }
        return null
    }

    companion object {
        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val world = player.world
            if (
                world.getBlockState(pos.north().west()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.north().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().west()).isOf(Blocks.FARMLAND)
            ) {
                val farm = Farm(pos, pos, 1)
                Response.NEW_STRUCTURE.send(player, farm.type.name)
                return farm
            } else {
                Response.NOT_ENOUGHT_MOISTURE.send(player)
                return null
            }
        }
    }
}
