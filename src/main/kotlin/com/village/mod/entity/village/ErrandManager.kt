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
    private var freeErrands: (() -> List<Errand>?)? = null,
) {
    private fun calculatePriority(errand: Errand): Byte =
        errand.let { (cid, p) ->
            Action.get(cid).scan(entity, p)
        }

    private val errandComparator =
        Comparator<Errand> { e1, e2 ->
            val v = calculatePriority(e1)
            val u = calculatePriority(e2)
            u.compareTo(v)
        }

    private val queue = PriorityQueue(errandComparator)

    var self: Int = 0
    var home: Int = 0
    var work: Int = 0
    var free: Int = 0
    var sdim: Byte = 0

    fun peek(): Errand? {
        if (queue.isEmpty()) {
            return null
        }
        return queue.peek()
    }

    fun has(type: Action.Type): Boolean = queue.any { it.cid == type }

    fun pop(): Errand {
        val e = queue.poll()
        LOGGER.info("This errand was popped: {}", e)
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
            val list = errands.invoke(self)
            list?.let {
                combinedErrands.addAll(list)
            }
        }
        workErrands?.let { errands ->
            val list = errands.invoke(self)
            list?.let {
                combinedErrands.addAll(list)
            }
        }
        freeErrands?.let { errands ->
            val list = errands.invoke()
            list?.let {
                combinedErrands.addAll(list)
            }
        }

        val filtered = combinedErrands.filter { calculatePriority(it) > 0.0 }
        queue.addAll(filtered)
    }

    private fun printQueue() {
        queue.forEach { e ->
            LOGGER.info("| -> Action: {}, Pos: {}", e.cid, e.pos)
        }
    }

    fun update() {
        LOGGER.info("My key is {}", self)
        LOGGER.info("My work is {}", work)
        LOGGER.info("My home is {}", home)
        updatePriorityQueue()
        printQueue()
    }

    fun assignSettlement(
        id: Int,
        key: Int,
        errands: (() -> List<Errand>?)?,
    ) {
        freeErrands = errands
        self = key
        free = id
    }

    fun attachSettlement(errands: (() -> List<Errand>?)?) {
        freeErrands = errands
    }

    fun assignStructure(
        id: Int,
        errands: ((Int) -> List<Errand>?)?,
        isHouse: Boolean,
    ) {
        if (isHouse) {
            homeErrands = errands
            home = id
        } else {
            workErrands = errands
            work = id
        }
    }

    fun attachStructure(
        errands: ((Int) -> List<Errand>?)?,
        isHouse: Boolean,
    ) {
        if (isHouse) {
            homeErrands = errands
        } else {
            workErrands = errands
        }
    }

    fun hasHome(): Boolean = homeErrands != null

    fun hasWork(): Boolean = workErrands != null

    fun hasSettlement(): Boolean = freeErrands != null

    fun hasErrand(errand: Errand): Boolean = queue.contains(errand)

    fun hasErrands(): Boolean = !queue.isEmpty()
}
