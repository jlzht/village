package com.settlement.mod.entity.mob

import com.settlement.mod.LOGGER
import com.settlement.mod.action.Action
import com.settlement.mod.action.Errand
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import java.util.PriorityQueue

// TODO: add errand serialization
class ErrandManager {
    private val comparator =
        Comparator<Errand> { e1, e2 ->
            e1.priority.compareTo(e2.priority)
        }

    private val queue = PriorityQueue(comparator)

    fun peek(): Errand? = queue.peek()

    fun has(type: Action.Type): Boolean = queue.any { it.cid == type }

    fun isEmpty(): Boolean = queue.isEmpty()

    fun pop(): Errand? {
        val e = queue.poll()
        e?.let { LOGGER.info("Popped errand: {}", e) }
        return e
    }

    fun push(
        action: Action.Type,
        pos: BlockPos?,
        priority: Byte,
    ): Boolean {
        if (priority > 0) {
            val errand = Errand(action, pos, priority)
            LOGGER.info("Added errand: {}", errand)
            queue.add(errand)
            return true
        }
        return false
    }

    fun clear() {
        queue.clear()
    }

    fun toNbt(): NbtCompound =
        NbtCompound().apply {
        }

    companion object {
        fun fromNbt(nbt: NbtCompound): ErrandManager = ErrandManager()
    }
}
