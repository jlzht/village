package com.village.mod.util

import net.minecraft.util.math.BlockPos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

//
object BlockIterator {
    private val neighboursOffsets =
        setOf(
            BlockPos(1, 0, 0), // east
            BlockPos(-1, 0, 0), // west
            BlockPos(0, 0, 1), // south
            BlockPos(0, 0, -1), // north
        )

    val NEIGHBOURS: (BlockPos) -> List<BlockPos> = { b ->
        neighboursOffsets.map { offset -> b.add(offset) }
    }
    private val touchingOffsets =
        setOf(
            BlockPos(0, 0, -1), // north
            BlockPos(0, 0, 1), // south
            BlockPos(1, 0, 0), // east
            BlockPos(-1, 0, 0), // west
            BlockPos(0, 1, 0), // up
            BlockPos(0, -1, 0), // down
        )

    val TOUCHING: (BlockPos) -> List<BlockPos> = { b ->
        touchingOffsets.map { offset -> b.add(offset) }
    }

    private val bottomOffsets =
        setOf(
            BlockPos(0, -1, 0),
            BlockPos(0, -1, -1),
            BlockPos(0, -1, 1),
            BlockPos(1, -1, 0),
            BlockPos(-1, -1, 0),
            BlockPos(1, -1, -1),
            BlockPos(-1, -1, -1),
            BlockPos(1, -1, 1),
            BlockPos(-1, -1, 1),
        )

    val BOTTOM: (BlockPos) -> List<BlockPos> = { b ->
        bottomOffsets.map { offset -> b.add(offset) }
    }

    val CUBOID: (BlockPos, BlockPos) -> Iterable<BlockPos> = { lower, upper ->
        Iterable {
            object : AbstractIterator<BlockPos>() {
                private var x = lower.x - 1
                private var z = lower.z - 1

                override fun computeNext() {
                    if (z > upper.z + 1 || x > upper.x + 1) {
                        done()
                        return
                    }

                    val point = BlockPos(x, lower.y, z)
                    setNext(point)

                    x++
                    if (x > upper.x) {
                        x = lower.x
                        z++
                    }
                }
            }
        }
    }

    val CIRCUMFERENCE: (BlockPos, Int) -> Iterable<BlockPos> = { center, radius ->
        Iterable {
            object : AbstractIterator<BlockPos>() {
                private var angle = 0.0
                private val angleIncrement = 360.0 / (30 * radius)

                override fun computeNext() {
                    if (angle >= 360) {
                        done()
                        return
                    }
                    val xOffset = (Math.cos(Math.toRadians(angle)) * radius).roundToInt()
                    val zOffset = (Math.sin(Math.toRadians(angle)) * radius).roundToInt()
                    setNext(center.add(xOffset, 0, zOffset))
                    angle += angleIncrement
                }
            }
        }
    }
}
