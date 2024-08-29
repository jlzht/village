package com.village.mod.utils

import net.minecraft.util.math.BlockPos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Utils {
    fun iterateInCircuference(center: BlockPos, radius: Int): Iterable<BlockPos> {
        return Iterable {
            object : AbstractIterator<BlockPos>() {
                private var angle = 0.0
                private val angleIncrement = 360.0 / (30 * radius)

                override fun computeNext() {
                    if (angle >= 360) {
                        done()
                        return // endOfData()
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
