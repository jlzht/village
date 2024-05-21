package com.village.mod.entity.village

import com.village.mod.village.villager.Action
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER

data class Errand(val pos: BlockPos, val action: Action)


class ActionManager {
    private val targetBlock: HashSet<Errand> = hashSetOf()

    fun push(block: Errand) {
        LOGGER.info("Adding action for block at: ${block.pos} - Action: ${block.action}")
        targetBlock.add(block)
    }

    fun push(blocks: Collection<Errand>) {
        targetBlock.addAll(blocks)
        LOGGER.info("Added ${blocks.size} actions to the ActionManager.")
    }

    fun pop(block: Errand) {
        targetBlock.remove(block)
    }

    fun peek(): Errand {
        return targetBlock.first()
    }

    fun isEmpty(): Boolean {
        return targetBlock.isEmpty()
    }
}
