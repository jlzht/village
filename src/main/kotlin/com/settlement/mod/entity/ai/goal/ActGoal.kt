package com.settlement.mod.entity.ai.goal

import com.settlement.mod.LOGGER
import com.settlement.mod.action.Action
import com.settlement.mod.action.Errand
import com.settlement.mod.action.Position
import com.settlement.mod.action.Parallel
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.util.Finder
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.math.BlockPos
import java.util.EnumSet

class ActGoal(
    private val entity: AbstractVillagerEntity,
) : Goal() {
    private val world = entity.world

    val parallelMap =
        mapOf(
            Action.Type.FLEE to setOf(Pair(Action.Type.EAT, 7)),
            Action.Type.LOOK to setOf(Pair(Action.Type.TALK, 6)),
        )

    private var tickToTestCount = 0
    private var tickToExecCount = 0
    private var ticksWithoutPath = 0

    private var path: Path? = null
    private var errand: Errand? = null
    private var parallel: Errand? = null

    private var primaryState = ErrandState.PENDING
    private var parallelState = ErrandState.PENDING

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.errandManager.isEmpty()) return false
        val peek = entity.errandManager.peek()
        if (peek != null && errand != peek) {
            resetErrand()
            errand = peek
            if (parallel == null) {
                parallelMap[peek.cid]
                    ?.firstOrNull { (pid, chance) ->
                        entity.random.nextInt(chance) == 0 && (Action.get(pid) as Parallel).scan(entity) > peek.priority
                    }?.let { (pid, _) ->
                        parallel = Errand(pid)
                    }
            }
        }
        return peek != null
    }

    override fun shouldContinue(): Boolean = false

    override fun shouldRunEveryTick() = true

    override fun tick() {
        errand?.let { processPrimary(it, primaryState, { state -> setPrimaryState(state) }, { resetPrimaryState() }) }
        parallel?.let { processParallel(it, parallelState, { state -> setParallelState(state) }, { resetParallelState() }) }
    }

    private fun processPrimary(
        errand: Errand,
        current: ErrandState,
        setState: (ErrandState) -> Unit,
        resetState: () -> Unit,
    ) {
        val (cid, pos) = errand
        val action = Action.get(cid) as Position
        val distance = calculateDistanceAndPath(pos, action)

        if (action.shouldMove(distance)) {
            tickMovement(cid, action)
        } else {
            tickToTestCount++
        }

        when (current) {
            ErrandState.PENDING -> tickPending(action, errand, setState, resetState)
            ErrandState.TESTED -> tickTested(action, errand, setState, resetState)
            ErrandState.COMPLETED -> tickCompleted(action, errand, setState, resetState)
        }
    }

    private fun processParallel(
        errand: Errand,
        current: ErrandState,
        setState: (ErrandState) -> Unit,
        resetState: () -> Unit,
    ) {
        val (cid, _) = errand
        val action = Action.get(cid)

        when (current) {
            ErrandState.PENDING -> tickPending(action, errand, setState, resetState)
            ErrandState.TESTED -> tickTested(action, errand, setState, resetState)
            ErrandState.COMPLETED -> tickCompleted(action, errand, setState, resetState)
        }
    }

    private fun tickPending(
        action: Action,
        errand: Errand,
        setState: (ErrandState) -> Unit,
        resetState: () -> Unit,
    ) {
        if (action.shouldTest(tickToTestCount)) {
            if (action is Position) {
                if (action.test(entity, errand.pos) >= 1) {
                    LOGGER.info("> Test passed")
                    setState(ErrandState.TESTED)
                } else {
                    LOGGER.info("> Test failed")
                    resetState()
                }
            }
            if (action is Parallel) {
                if (action.test(entity) >= 1) {
                    LOGGER.info("> Test passed")
                    setState(ErrandState.TESTED)
                } else {
                    LOGGER.info("> Test failed")
                    resetState()
                }
            }
        }
    }

    private fun tickTested(
        action: Action,
        errand: Errand,
        setState: (ErrandState) -> Unit,
        resetState: () -> Unit,
    ) {
        if (action.shouldExec(tickToExecCount)) {
            LOGGER.info("> Action executed")
            if (action is Position) {
                entity.energy -= action.energyCost
                action.exec(entity, errand.pos)
                setState(ErrandState.COMPLETED)
            }
            if (action is Parallel) {
                action.exec(entity)
                setState(ErrandState.COMPLETED)
            }
        } else {
            tickToExecCount++
        }
    }

    private fun tickCompleted(
        action: Action,
        errand: Errand,
        setState: (ErrandState) -> Unit,
        resetState: () -> Unit,
    ) {
        if (action is Position) {
            when (action.eval(entity, errand.pos)) {
                1.toByte() -> {
                    LOGGER.info("> Errand concluded")
                    resetState()
                }
                2.toByte() -> {
                    resetState()
                    action.redo(entity, errand.pos)
                }
            }
        }
        if (action is Parallel) {
            when (action.eval(entity)) {
                1.toByte() -> {
                    LOGGER.info("> Errand concluded")
                    resetState()
                }
                2.toByte() -> {
                    resetState()
                    action.redo(entity)
                }
            }
        }
    }

    private fun calculateDistanceAndPath(
        pos: BlockPos?,
        action: Position,
    ): Double =
        entity.target?.let { target ->
            val dist = entity.squaredDistanceTo(target)
            if (action.shouldLook(dist)) entity.getLookControl().lookAt(target)
            if (entity.isAlive && entity.isAttacking()) {
                entity.navigation.findPathTo(target, 1)?.let { path = it }
                dist
            } else {
                null
            }
        } ?: run {
            pos?.let { point ->
                val center = point.toCenterPos()
                val dist = entity.squaredDistanceTo(center)
                if (action.shouldLook(dist)) entity.getLookControl().lookAt(center)
                entity.navigation.findPathTo(center.x, center.y, center.z, 0)?.let { path = it }
                dist
            } ?: 0.0
        }

    private fun tickMovement(
        cid: Action.Type,
        action: Position,
    ) {
        path?.let {
            if (cid !in setOf(Action.Type.OPEN, Action.Type.CLOSE)) {
                Finder.findDoorBlock(entity.world, it)?.let { (cid, pos) ->
                    entity.pushErrand(cid, pos)
                    return
                }
            }
            entity.getUp()
            entity.navigation.startMovingAlong(path, 1.0)
        } ?: entity.navigation.stop()

        if (entity.navigation.isIdle) {
            if (++ticksWithoutPath >= 50) {
                entity.errandManager.pop()
            }
        } else {
            entity.getNavigation().setSpeed(if (entity.isUsingItem) 0.5 else action.speedModifier)
            ticksWithoutPath = 0
        }
    }

    private fun setParallelState(state: ErrandState) {
        parallelState = state
    }

    private fun setPrimaryState(state: ErrandState) {
        primaryState = state
    }

    private fun resetParallelState() {
        parallelState = ErrandState.PENDING
        parallel = null
    }

    private fun resetPrimaryState() {
        entity.errandManager.pop()
        entity.navigation.stop()
        resetErrand()
    }

    private fun resetErrand() {
        tickToTestCount = 0
        tickToExecCount = 0
        primaryState = ErrandState.PENDING
        errand = null
        path = null
    }

    private enum class ErrandState {
        PENDING,
        TESTED,
        COMPLETED,
    }
}
