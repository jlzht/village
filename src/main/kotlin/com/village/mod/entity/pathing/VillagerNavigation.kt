package com.village.mod.entity.ai.pathing

import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.ai.pathing.MobNavigation
import net.minecraft.entity.ai.pathing.LandPathNodeMaker
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.entity.ai.pathing.PathNodeNavigator
import net.minecraft.entity.ai.pathing.PathNodeType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class VillagerNavigation(entity: CustomVillagerEntity, world: World) : MobNavigation(entity, world) {

    override fun createPathNodeNavigator(range: Int): PathNodeNavigator {
        this.nodeMaker = LandPathNodeMaker().apply {
            setCanEnterOpenDoors(true)
        }
        return PathNodeNavigator(this.nodeMaker, range)
    }

    override fun isAtValidPosition(): Boolean {
        return this.entity.isOnGround || this.entity.isInFluid || this.entity.hasVehicle()
    }

    override fun getPos(): Vec3d {
        return Vec3d(this.entity.x, this.pathfindingY.toDouble(), this.entity.z)
    }

    override fun findPathTo(target: BlockPos, distance: Int): Path? {
        var tar = target
        var blockPos: BlockPos
        val worldChunk = this.world.getChunkManager().getWorldChunk(ChunkSectionPos.getSectionCoord(target.x), ChunkSectionPos.getSectionCoord(target.z))
            ?: return null
        if (worldChunk.getBlockState(target).isAir) {
            blockPos = target.down()
            while (blockPos.y > this.world.bottomY && worldChunk.getBlockState(blockPos).isAir) {
                blockPos = blockPos.down()
            }
            if (blockPos.y > this.world.bottomY) {
                return super.findPathTo(blockPos.up(), distance)
            }
            while (blockPos.y < this.world.topY && worldChunk.getBlockState(blockPos).isAir) {
                blockPos = blockPos.up()
            }
            tar = blockPos
        }
        if (!worldChunk.getBlockState(tar).isAir) {
            blockPos = target.up()
            while (blockPos.y < this.world.topY && !worldChunk.getBlockState(blockPos).isAir) {
                blockPos = blockPos.up()
            }
            return super.findPathTo(blockPos, distance)
        }
        return super.findPathTo(tar, distance)
    }

    override fun findPathTo(entity: Entity, distance: Int): Path? {
        return this.findPathTo(entity.blockPos, distance)
    }

    private val pathfindingY: Int
        get() {
            if (!this.entity.isTouchingWater || !this.canSwim()) {
                return MathHelper.floor(this.entity.y + 0.5)
            }
            var i: Double = this.entity.blockY.toDouble()
            var blockState: BlockState = this.world.getBlockState(BlockPos.ofFloored(this.entity.x, i, this.entity.z))
            var j = 0
            while (blockState.isOf(Blocks.WATER)) {
                i++
                blockState = this.world.getBlockState(BlockPos.ofFloored(this.entity.x, i, this.entity.z))
                if (++j > 16) {
                    return this.entity.blockY
                }
            }
            return i.toInt()
        }
}
