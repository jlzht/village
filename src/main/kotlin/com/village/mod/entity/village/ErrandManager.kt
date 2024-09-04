package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import net.minecraft.util.math.BlockPos
import java.util.PriorityQueue

class ErrandManager(
    private val entity: CustomVillagerEntity,
    private var homeErrands: ((Int) -> List<Errand>?)? = null,
    private var workErrands: ((Int) -> List<Errand>?)? = null,
) {
    private fun calculatePriority(errand: Errand): Double =
        errand.let { (cid, p) ->
            Action.get(cid).scan(entity, p).toDouble()
        }

    private val errandComparator =
        Comparator<Errand> { e1, e2 ->
            val v = calculatePriority(e1)
            val u = calculatePriority(e2)
            u.compareTo(v)
        }

    private val queue = PriorityQueue(errandComparator)

    var homeID: Int = -1
    var workID: Int = -1

    fun peek(): Errand? {
        if (queue.isEmpty()) {
            return null
        }
        return queue.peek()
    }

    fun has(type: Action.Type): Boolean = queue.any { it.cid == type }

    fun pop(): Errand {
        val e = queue.poll()
        LOGGER.info("THIS ERRAND WAS POPPED: {}", e)
        return e
    }

    fun add(
        action: Action.Type,
        pos: BlockPos?,
    ): Boolean = add(Errand(action, pos))

    fun add(action: Action.Type): Boolean = add(action, null)

    fun add(errand: Errand): Boolean {
        if (calculatePriority(errand) > 0.0) {
            LOGGER.info("Added errand: {}", errand)
            queue.add(errand)
            return true
        } else {
            return false
        }
    }

    private fun updatePriorityQueue() {
        val combinedErrands = mutableListOf<Errand>()
        homeErrands?.let { errands ->
            val list = errands.invoke(entity.data.key)
            LOGGER.info(">>>> {}", list)
            list?.let {
                combinedErrands.addAll(list)
            }
        }
        workErrands?.let { errands ->
            val list = errands.invoke(entity.data.key)
            LOGGER.info(">>>> {}", list)
            list?.let {
                combinedErrands.addAll(list)
            }
        }
        LOGGER.info("|> Combined Errands: {}", combinedErrands)
        val filteredErrands = combinedErrands.filter { calculatePriority(it) > 0.0 }
        queue.addAll(filteredErrands)
    }

    private fun printQueue() {
        queue.forEach { e ->
            LOGGER.info("| -> Action: {}, Pos: {}", e.cid, e.pos)
        }
    }

    fun update() {
        updatePriorityQueue()
        printQueue()
    }

    fun assignStructure(
        id: Int,
        errands: ((Int) -> List<Errand>?)?,
        isHouse: Boolean,
    ) {
        if (isHouse) {
            homeErrands = errands
            homeID = id
        } else {
            workErrands = errands
            workID = id
        }
    }

    fun hasHome(): Boolean = homeErrands != null

    fun hasWork(): Boolean = workErrands != null

    fun hasErrand(errand: Errand): Boolean = queue.contains(errand)

    fun hasErrands(): Boolean = !queue.isEmpty()
}
