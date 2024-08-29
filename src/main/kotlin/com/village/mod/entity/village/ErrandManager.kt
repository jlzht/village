package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import net.minecraft.util.math.BlockPos
import java.util.PriorityQueue

class ErrandManager(
    private val entity: CustomVillagerEntity,
    private var homeErrands: (Int) -> List<Errand>?,
    private var workErrands: (Int) -> List<Errand>?,
    private val calculatePriority: (Errand) -> Double,
) {

    private val errandComparator = Comparator<Errand> { e1, e2 ->
        calculatePriority(e1).compareTo(calculatePriority(e2))
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

    fun pop(): Errand {
        val e = queue.poll()
        LOGGER.info("THIS ERRAND WAS POPPED: {}", e)
        return e
    }

    fun add(action: Action.Type, pos: BlockPos): Boolean {
        val errand = Errand(action, pos)
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
        val home = homeErrands(entity.key)
        if (home != null) {
            LOGGER.info("Home: {}", home.iterator())
            combinedErrands.addAll(home)
        }
        val work = workErrands(entity.key)
        if (work != null) {
            LOGGER.info("Work: {}", work.iterator())
            combinedErrands.addAll(work)
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

    fun assignStructure(id: Int, errands: (Int) -> List<Errand>?, isHouse: Boolean) {
        if (isHouse) {
            homeErrands = errands
            homeID = id
        } else {
            workErrands = errands
            workID = id
        }
    }

    fun hasHome(): Boolean {
        return homeErrands(entity.key) != null
    }

    fun hasWork(): Boolean {
        return workErrands(entity.key) != null
    }

    fun hasErrand(errand: Errand): Boolean {
        return queue.contains(errand)
    }

    fun hasErrands(): Boolean {
        return !queue.isEmpty()
    }
}
