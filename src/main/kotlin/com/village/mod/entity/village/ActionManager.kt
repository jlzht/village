package com.village.mod.entity.village

import com.village.mod.village.villager.Action
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER
import java.util.Stack

data class Errand(val pos: BlockPos, val action: Action)


class ActionManager {
    private val targetBlock: Stack<Errand> = Stack<Errand>()

    fun get(): List<Errand> {
        return targetBlock.toList()
    }

    fun push(block: Errand) {
        LOGGER.info("Adding action for block at: ${block.pos} - Action: ${block.action}")
        targetBlock.push(block)
    }

    fun push(blocks: Collection<Errand>) {
        targetBlock.addAll(blocks)
        LOGGER.info("Added ${blocks.size} actions to the ActionManager.")
    }

    fun pop(block: Errand) {
        targetBlock.remove(block)
    }

    fun peek(): Errand {
        return targetBlock.peek()
    }
    fun popp() {
        targetBlock.pop()
    }
    fun contains(block: Errand): Boolean {
        return targetBlock.contains(block)
    }

    fun isEmpty(): Boolean {
        return targetBlock.empty()
    }
}
