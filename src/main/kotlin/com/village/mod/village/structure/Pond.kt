package com.village.mod.village.structure

import com.village.mod.world.graph.Node
import com.village.mod.screen.Response
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos

class Pond(lower: BlockPos, upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.POND
    override var capacity: Int = 1
    override var area: Region = Region(lower, upper)

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
                Response.SMALL_BODY_WATER.send(player)
                return null
            }
            val wpos = water.get()
            val pond = Pond(wpos.south().west(), wpos.north().east())
            pond.graph.addNode(Node(5, wpos, 0.5f, emptyList()))
            Response.NEW_STRUCTURE.send(player, pond.type.name)
            return pond
        }
    }
}
