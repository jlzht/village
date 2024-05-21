package com.village.mod.village.structure

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.block.Block
import net.minecraft.block.BarrelBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock

//import com.village.mod.village.structure.Building
//import com.village.mod.village.structure.Farm
//import com.village.mod.village.structure.Point
//import com.village.mod.village.structure.Pond
//import com.village.mod.village.structure.Region


object StructureFactory {
    fun createStructure(block: Block, pos: BlockPos, player: PlayerEntity): Structure? {
        return when (block) {
            is FarmlandBlock -> Farm.createStructure(pos, player)
            is BarrelBlock -> Pond.createStructure(pos, player)
            is DoorBlock -> Building.createStructure(pos, player)
            else -> null
        }
    }
}
