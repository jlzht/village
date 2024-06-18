package com.village.mod.village.structure

import com.village.mod.screen.Response
import com.village.mod.village.villager.Action
import com.village.mod.world.graph.Node
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class Farm(val lower: BlockPos, val upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.FARM
    override var capacity: Int = 1
    override var area: Region = Region(lower, upper)

    // when farmers can work they will ask for structure entrance Graph#navigate, if structure exist, onAccess is called
    override fun onAccess(world: World) {
        super.onAccess(world)
        this.checkCrops(world)
    }

    private fun checkCrops(world: World) {
        // SQUARE ITERATION?
        val farmCenter = area.center()
        for (x in (area.lower.x - 1)..(area.upper.x + 1)) {
            for (z in (area.lower.z - 1)..(area.upper.z + 1)) {
                val farmPos = BlockPos(x, farmCenter.y, z)
                if (farmCenter.getManhattanDistance(farmPos) < 12.0f) {
                    val action = getFarmAction(world, farmPos, world.getBlockState(farmPos))
                    if (action != Action.PASS && action != Action.NONE) {
                        if (action != Action.BREAK) {
                            // future work
                        } else {
                            // future work
                        }
                    }
                    // TODO: add check of decrease size of farmland
                    if (!area.contains(farmPos)) {
                        area.expand(farmPos)
                    }
                }
            }
        }
    }

    private fun getFarmAction(world: World, pos: BlockPos, block: BlockState): Action {
        if (block.isOf(Blocks.FARMLAND) && block.get(FarmlandBlock.MOISTURE) >= 5) {
            if (world.getBlockState(pos.up()).isOf(Blocks.AIR)) {
                return Action.PLANT
            } else {
                val up = world.getBlockState(pos.up())
                val upBlock = up.getBlock()
                if (upBlock is CropBlock && upBlock.isMature(up)) {
                    return Action.BREAK
                }
                return Action.PASS
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
                        ) && (
                        (west.isOf(Blocks.FARMLAND) && west.get(FarmlandBlock.MOISTURE) >= 5) ||
                            (east.isOf(Blocks.FARMLAND) && east.get(FarmlandBlock.MOISTURE) >= 5)
                        )
                ) {
                    val northwest = world.getBlockState(pos.north().west())
                    val northeast = world.getBlockState(pos.north().east())
                    val southeast = world.getBlockState(pos.south().east())
                    val southwest = world.getBlockState(pos.south().west())
                    if (
                        (northeast.isOf(Blocks.FARMLAND) || northeast.isOf(Blocks.WATER)) ||
                        (northwest.isOf(Blocks.FARMLAND) || northwest.isOf(Blocks.WATER)) ||
                        (southeast.isOf(Blocks.FARMLAND) || southeast.isOf(Blocks.WATER)) ||
                        (southwest.isOf(Blocks.FARMLAND) || southwest.isOf(Blocks.WATER))
                    ) {
                        return Action.TILL
                    }
                }
            }
        }
        return Action.NONE
    }

    companion object {
        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val world = player.world
            if (
                world.getBlockState(pos.north().west()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.north().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().west()).isOf(Blocks.FARMLAND)
            ) {
                val farm = Farm(pos, pos)
                farm.graph.addNode(Node(5, farm.area.center().up(), 0.5f, emptyList()))
                Response.NEW_STRUCTURE.send(player, farm.type.name)
                return farm
            } else {
                Response.NOT_ENOUGHT_MOISTURE.send(player)
                return null
            }
        }
    }
}
