package com.village.mod.world.graph

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.block.Blocks
import net.minecraft.block.StairsBlock
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import java.util.PriorityQueue

object NodeUtils {
    fun findPath(world: World, start: BlockPos, goal: BlockPos, maxDistance: Int): BlockPos? {
        val openSet = PriorityQueue<BlockPos>(compareBy { getFCost(world, it, goal) })
        val gScore = mutableMapOf<BlockPos, Double>().withDefault { Double.MAX_VALUE }
        val fScore = mutableMapOf<BlockPos, Double>().withDefault { Double.MAX_VALUE }
        var sizeNeight = 0
        gScore[start] = 0.0
        fScore[start] = getHeuristic(start, goal)
        openSet.add(start)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()
            if (current == goal) {
                LOGGER.info("{} REACHES: {}", start, goal)
                return null
            }
            if (start.getManhattanDistance(current) > maxDistance) {
                return current
            }
            val kst = getNeighbors(world, current)
            if (sizeNeight != kst.size) {
                if (sizeNeight == 1) {
                    return current
                } else {
                    sizeNeight = kst.size
                }
            }
            for (neighbor in kst) {
                val tentativeGScore = gScore.getValue(current)
                if (tentativeGScore < gScore.getValue(neighbor)) {
                    gScore[neighbor] = tentativeGScore + getFCost(world, neighbor, goal)
                    fScore[neighbor] = tentativeGScore + getHeuristic(neighbor, goal)
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor)
                    }
                }
            }
        }
        return BlockPos(0, 0, 0)
    }

    private fun getFCost(world: World, pos: BlockPos, goal: BlockPos): Double {
        return getHeuristic(pos, goal)
    }

    public fun getDistance(sr: BlockPos, sl: BlockPos): Double {
        val f = sr.x - sl.x.toFloat()
        val g = sr.y - sl.y.toFloat()
        val h = sr.z - sl.z.toFloat()
        return MathHelper.sqrt(f * f + g * g + h * h).toDouble()
    }

    private fun getHeuristic(pos1: BlockPos, pos2: BlockPos): Double {
        return getDistance(pos1, pos2)
    }

    private fun getNeighbors(world: World, pos: BlockPos): List<BlockPos> {
        val k = world.getBlockState(pos.down())
        if (k.getBlock() is StairsBlock) {
            return listOf(pos.down().offset(k.get(StairsBlock.FACING).opposite, 1))
        }
        val c = listOf(
            pos.north().down(),
            pos.south().down(),
            pos.west().down(),
            pos.east().down(),
        )
        val b = listOf(
            pos.north(),
            pos.south(),
            pos.west(),
            pos.east(),
        )
        val a = listOf(
            pos.north().up(),
            pos.south().up(),
            pos.west().up(),
            pos.east().up(),
        )
        val l = mutableListOf<BlockPos>()
        for (i in 0 until 4) {
            if (isWalkable(world, a[i])) { l.add(a[i]); continue }
            if (isWalkable(world, b[i])) { l.add(b[i]); continue }
            if (isWalkable(world, c[i])) { l.add(c[i]); continue }
        }
        return l
    }

    private fun isWalkable(world: World, pos: BlockPos): Boolean {
        return (!world.getBlockState(pos.down()).isAir && !world.getBlockState(pos.down()).isIn(BlockTags.FENCES)) &&
            world.getBlockState(pos).isAir &&
            world.getBlockState(pos.up()).isAir
    }
}

enum class Callback() {
    SLEEP() {
        override fun invoke(pos: BlockPos): ((CustomVillagerEntity) -> Unit) = {
                e ->
            e.sleep(pos.down())
        }
    }, ;
    abstract operator fun invoke(pos: BlockPos): ((CustomVillagerEntity) -> Unit)
    companion object {
        fun get(id: Int): Callback {
            return values().get(id)
        }
    }
}

// enum class Action {
//    PLANT,
//    FORGE,
//    GRIND,
//    STORE,
//    BREAK,
//    NONE,
//    TALK,
//    PASS,
//    MOVE,
//    TILL,
//    BREW,
//    BAKE,
//    POUR,
//    FISH,
//    OPEN,
//    CLOSE,
//    SIT
// }
