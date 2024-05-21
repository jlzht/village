package com.village.mod.village.structure


import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class Farm(val lower: BlockPos, val upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.FARM
    override var capacity: Int = 1
    override var area: Area = Region(lower, upper)
    override var errands: HashSet<Errand> = hashSetOf()

    override fun peelErrands(): List<Errand> {
        val aux = errands.take(8)
        errands.removeAll(aux)
        return aux
    }

    private fun getFarmAction(world: World, pos: BlockPos, block: BlockState): Action {
        if (block.isOf(Blocks.FARMLAND) && block.get(FarmlandBlock.MOISTURE) >= 5) {
            if (world.getBlockState(pos.up()).isOf(Blocks.AIR)) {
                return Action.PLANT
            } else {
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

    override fun genErrands(world: World) {
        if (errands.isEmpty()) {
            val region = (this.area as Region)
            val farmCenter = region.center()
            // SQUARE ITERATION?
            for (x in (region.lower.x - 1)..(region.upper.x + 1)) {
                for (z in (region.lower.z - 1)..(region.upper.z + 1)) {
                    val farmPos = BlockPos(x, farmCenter.y, z)
                    if (farmCenter.getManhattanDistance(farmPos) < 12.0f) {
                        val action = getFarmAction(world, farmPos, world.getBlockState(farmPos))
                        if (action != Action.PASS && action != Action.NONE) {
                            errands.append(farmPos, action)
                        }
                        if (!region.contains(farmPos)) {
                            region.expand(farmPos)
                        }
                    }
                }
            }
        }
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
                return Farm(pos, pos)
            } else {
                return null
            }
        }
    }
}
