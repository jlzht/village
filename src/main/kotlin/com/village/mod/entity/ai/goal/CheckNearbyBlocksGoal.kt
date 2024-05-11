package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.ProfessionType
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.EnumSet
import org.joml.Math

class CheckNearbyBlocksGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world
    private var lastUpdateTime: Long = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
    //    val currentTime = world.time
    //    if (currentTime - lastUpdateTime < 150L) {
    //        return false
    //    }
    //    lastUpdateTime = currentTime
        return true
    }

    override fun shouldContinue(): Boolean {
        return false
    }

    override fun start() {
    //    val pos = entity.getBlockPos()
    //    val aPos = if (world.getBlockState(pos).isOf(Blocks.FARMLAND)) {
    //        pos
    //    } else {
    //        pos.down()
    //    }
    //    val professionType = entity.getProfession().type
    //    for (blockPos in BlockPos.iterate(aPos.add(-5, -1, -5), aPos.add(5, 1, 5))) {
    //        if (professionType == ProfessionType.FARMER) {
    //             //if (pos.y == blockPos.y) {
    //                  if (this.isValidFarmland(blockPos)) {
    //                    entity.setTargetBlock(blockPos)
    //                    break
    //                  }
    //            //}
    //        }
    //        if (professionType == ProfessionType.FISHERMAN) {
    //            //if (pos.y > blockPos.y) {
    //                if (Math.abs(pos.x - blockPos.x) < 3 && Math.abs(pos.z - blockPos.z) < 3) {
    //                  if (this.isValidFishingWater(blockPos)) {
    //                    entity.setTargetBlock(blockPos)
    //                    break
    //                  }
    //                }
    //            //}
    //        }
    //    }
    }

    override fun stop() {
    }

    private fun isValidFishingWater(blockPos: BlockPos): Boolean {
    if (world.getBlockState(blockPos).isOf(Blocks.WATER)) {
        if (world.getBlockState(blockPos.up()).isOf(Blocks.AIR)) {
            if (world.getBlockState(blockPos.north()).isOf(Blocks.WATER)
                && world.getBlockState(blockPos.south()).isOf(Blocks.WATER)
                && world.getBlockState(blockPos.west()).isOf(Blocks.WATER)
                && world.getBlockState(blockPos.east()).isOf(Blocks.WATER)
            ) {
                if (world.getBlockState(blockPos.down()).isOf(Blocks.WATER)
                ) {
                    return true
                }
            }
        }
    }
    return false
    }

    private fun isValidFarmland(blockPos: BlockPos): Boolean {
    //    if (world.getBlockState(blockPos).isOf(Blocks.GRASS_BLOCK) || world.getBlockState(blockPos).isOf(Blocks.DIRT)) {
    //        if (world.getBlockState(blockPos.up()).isOf(Blocks.AIR)) {
    //            if ((
    //                    (
    //                        world.getBlockState(blockPos.north()).isOf(Blocks.FARMLAND) ||
    //                            world.getBlockState(blockPos.north()).isOf(Blocks.WATER)
    //                        ) ||
    //                        (
    //                            world.getBlockState(blockPos.south()).isOf(Blocks.FARMLAND) ||
    //                                world.getBlockState(blockPos.south()).isOf(Blocks.WATER)
    //                            )
    //                    ) && (
    //                    world.getBlockState(blockPos.west()).isOf(Blocks.FARMLAND) ||
    //                        world.getBlockState(blockPos.east()).isOf(Blocks.FARMLAND)
    //                    )
    //            ) {
    //                if (world.getBlockState(blockPos.north().west()).isOf(Blocks.FARMLAND) ||
    //                    world.getBlockState(blockPos.north().east()).isOf(Blocks.FARMLAND) ||
    //                    world.getBlockState(blockPos.south().east()).isOf(Blocks.FARMLAND) ||
    //                    world.getBlockState(blockPos.south().west()).isOf(Blocks.FARMLAND)
    //                ) {
    //                    return true
    //                }
    //            }
    //        }
    //    }
        return false
    }
}
