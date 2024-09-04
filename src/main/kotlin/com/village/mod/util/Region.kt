package com.village.mod.util

import net.minecraft.util.math.BlockPos

data class Region(
    var lower: BlockPos,
    var upper: BlockPos,
) {
    fun append(block: BlockPos) {
        lower =
            BlockPos(
                if (block.x < lower.x) block.x else lower.x,
                if (block.y < lower.y) block.y else lower.y,
                if (block.z < lower.z) block.z else lower.z,
            )
        upper =
            BlockPos(
                if (block.x > upper.x) block.x else upper.x,
                if (block.y > upper.y) block.y else upper.y,
                if (block.z > upper.z) block.z else upper.z,
            )
    }

    fun grow() {
        lower = BlockPos(lower.x - 1, lower.y - 1, lower.z - 1)
        upper = BlockPos(upper.x + 1, upper.y + 1, upper.z + 1)
    }

    fun volume(): Int = (upper.x - lower.x + 1) * (upper.y - lower.y + 1) * (upper.z - lower.z + 1)

    fun center(): BlockPos {
        val middleX = (lower.x + upper.x) / 2
        val middleY = (lower.y + upper.y) / 2
        val middleZ = (lower.z + upper.z) / 2
        return BlockPos(middleX, middleY, middleZ)
    }

    fun contains(pos: BlockPos): Boolean =
        pos.x >= lower.x &&
            pos.x <= upper.x &&
            pos.y >= lower.y &&
            pos.y <= upper.y &&
            pos.z >= lower.z &&
            pos.z <= upper.z
}
