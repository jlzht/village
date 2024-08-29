package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Errand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

sealed class Structure(var capacity: Int) {
    abstract val MAX_CAPACITY: Int
    abstract val VOLUME_PER_RESIDENT: Int
    abstract val type: StructureType
    // rename to volume
    abstract var area: Region

    val errands = mutableListOf<Errand>()
    abstract val settlers: MutableList<Int>

    fun hasErrands(): Boolean = errands.isNotEmpty()
    abstract fun updateErrands(world: World)
    abstract fun getErrands(vid: Int): List<Errand>?
    abstract fun sortErrands()

    fun showErrands() {
        errands.forEach { e ->
            LOGGER.info("Action:{} - Pos:{}", e.cid, e.pos)
        }
    }

    fun isAvailable(): Boolean = settlers.count { it != -1 } <= capacity

    fun addResident(vid: Int) {
        settlers.set(settlers.indexOfFirst { it == -1 }, vid)
    }

    fun removeResident(vid: Int) {
        settlers.set(settlers.indexOf(vid), -1)
    }

    fun getResidents(): List<Int> {
        return settlers.filter { it != -1 }
    }
}

// TODO: Migrate this to own file, and rename area to volume(it is a 3D game)

data class Region(var lower: BlockPos, var upper: BlockPos) {
    fun expand(block: BlockPos) {
        lower = BlockPos(
            if (block.x < lower.x) block.x else lower.x,
            if (block.y < lower.y) block.y else lower.y,
            if (block.z < lower.z) block.z else lower.z,
        )
        upper = BlockPos(
            if (block.x > upper.x) block.x else upper.x,
            if (block.y > upper.y) block.y else upper.y,
            if (block.z > upper.z) block.z else upper.z,
        )
    }

    fun grow() {
        lower = BlockPos(lower.x - 1, lower.y - 1, lower.z - 1)
        upper = BlockPos(upper.x + 1, upper.y + 1, upper.z + 1)
    }

    fun area(): Int {
        return (upper.x - lower.x + 1) * (upper.y - lower.y + 1) * (upper.z - lower.z + 1)
    }

    fun center(): BlockPos {
        val middleX = (lower.x + upper.x) / 2
        val middleY = (lower.y + upper.y) / 2
        val middleZ = (lower.z + upper.z) / 2
        return BlockPos(middleX, middleY, middleZ)
    }

    fun contains(coord: BlockPos): Boolean {
        return coord.x >= lower.x && coord.x <= upper.x &&
            coord.y >= lower.y && coord.y <= upper.y &&
            coord.z >= lower.z && coord.z <= upper.z
    }

    // put this on the general utils object
    fun iterateSurface(): Iterable<BlockPos> {
        return Iterable {
            object : AbstractIterator<BlockPos>() {
                private var x = lower.x - 1
                private var z = lower.z - 1

                override fun computeNext() {
                    if (z > upper.z + 1 || x > upper.x + 1) {
                        done()
                        return
                    }

                    // Create the current point
                    val point = BlockPos(x, lower.y, z)
                    setNext(point)

                    // Update coordinates
                    x++
                    if (x > upper.x) {
                        x = lower.x
                        z++
                    }
                }
            }
        }
    }
}
