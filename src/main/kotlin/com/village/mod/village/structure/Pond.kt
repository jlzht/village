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

class Pond( lower: BlockPos, upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.POND
    override var capacity: Int = 1
    override var area: Region = Region(lower, upper)
    override var errands: HashSet<Errand> = hashSetOf()
    override fun peelErrands(): List<Errand> {
        return errands.toList()
    }
    override fun genErrands(world: World) {
        if (errands.isEmpty()) {
            val pond = area.center()
            if (world.getBlockState(pond).isOf(Blocks.WATER) && world.getBlockState(pond.up()).isOf(Blocks.AIR)) {
                errands.append(pond, Action.FISH)
            } else {
                // TODO: remove structure if check fail by adding signal to remove structures
            }
        }
    }

    companion object {
        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val world = player.world
            val water = BlockPos.findClosest(pos, 10, 5, { fpos ->
                world.getBlockState(fpos).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.up()).isOf(Blocks.AIR) &&
                    world.getBlockState(fpos.north()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.south()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.west()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.east()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.down()).isOf(Blocks.WATER)
            })
            if (!water.isPresent) {
                return null
            }
            val wpos = water.get()
            return Pond(wpos.south().west(), wpos.north().east())
        }
    }
}
