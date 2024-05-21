package com.village.mod.village.structure

import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

sealed class Structure() {
    open var owners: MutableList<Int> = mutableListOf()
    open var capacity: Int = 0

    abstract val type: StructureType
    abstract var area: Area

    open var errands: HashSet<Errand> = hashSetOf()
    abstract fun peelErrands(): List<Errand>
    abstract fun genErrands(world: World)
    fun HashSet<Errand>.append(pos: BlockPos, action: Action) {
        this.add(Errand(pos, action))
    }
    fun getErrands(world: World): List<Errand> {
        if (errands.isEmpty()) {
            this.genErrands(world)
            if (!errands.isEmpty()) {
                return peelErrands()
            }
        }
        return peelErrands()
    }
}

sealed class Area

data class Region(var lower: BlockPos, var upper: BlockPos) : Area() {
    fun expand(block: BlockPos) {
        this.lower = BlockPos(
            if (block.x < lower.x) block.x else lower.x,
            if (block.y < lower.y) block.y else lower.y,
            if (block.z < lower.z) block.z else lower.z,
        )
        this.upper = BlockPos(
            if (block.x > upper.x) block.x else upper.x,
            if (block.y > upper.y) block.y else upper.y,
            if (block.z > upper.z) block.z else upper.z,
        )
    }

    fun grow() {
        this.lower = BlockPos(lower.x - 1, lower.y - 1, lower.z - 1)
        this.upper = BlockPos(upper.x + 1, upper.y + 1, upper.z + 1)
    }
    fun area(): Int {
        return (this.upper.x - this.lower.x + 1) * (this.upper.y - this.lower.y + 1) * (this.upper.z - this.lower.z + 1)
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
}

data class Point(var point: BlockPos) : Area()
