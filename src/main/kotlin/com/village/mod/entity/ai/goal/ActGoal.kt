package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import java.util.EnumSet

class ActGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world

    private var tickToTestCount = 0
    private var tickToExecCount = 0

    private var path: Path? = null
    private var errand: Errand? = null

    private var state = ErrandState.PENDING

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (!entity.getErrandsManager().hasErrands()) return false

        val n = entity.getErrandsManager().peek()
        if (errand != n) {
            resetState()
            errand = n
        }

        if (path == null) {
            errand?.let { e ->
                e.pos?.let { p ->
                    val c = p.toCenterPos()
                    path = entity.navigation.findPathTo(c.x, c.y, c.z, 1)
                } ?: run {
                    entity.target?.let { t ->
                        path = entity.navigation.findPathTo(t, 1)
                    }
                }
            }
        }
        return path != null
    }

    override fun shouldContinue(): Boolean = false
    override fun shouldRunEveryTick() = true

    override fun tick() {
        errand?.let { (cid, pos) ->
            LOGGER.info("State: {}, Ticks: {}|{}", state, tickToTestCount, tickToExecCount)
            val action = Action.get(cid) // Check bytecode
            val distance = entity.target?.let { target ->
                val distance = entity.squaredDistanceTo(target)
                if (action.shouldLookAt(distance)) {
                    entity.getLookControl().lookAt(target)
                }
                if (entity.isAttacking() && target.isAlive) {
                    val npath = entity.navigation.findPathTo(target, 1)
                    if (npath != null) {
                        path = npath
                    }
                    distance
                } else {
                    return
                }
            } ?: run {
                pos?.let {
                    val point = it.toCenterPos()
                    var distance = entity.squaredDistanceTo(point)
                    if (action.shouldLookAt(distance)) {
                        entity.getLookControl().lookAt(point)
                    }
                    distance
                } ?: 0.0
            }

            if (action.shouldMoveTowards(distance)) {
                entity.navigation.startMovingAlong(path, if (entity.isUsingItem) { 0.5 } else { action.speedModifier })
            } else {
                tickToTestCount++
            }

            when (state) {
                ErrandState.PENDING -> {
                    if (tickToTestCount >= action.ticksToTest) {
                        if (action.test(entity, pos) > 0) {
                            LOGGER.info("Test passed")
                            state = ErrandState.TESTED
                        } else {
                            LOGGER.info("Test failed")
                            resetState()
                            entity.getErrandsManager().pop()
                            return
                        }
                    } else {
                        // TODO: if too many test ticks accumulates here, fails action
                    }
                }
                ErrandState.TESTED -> {
                    if (tickToExecCount >= action.ticksToExec) {
                        LOGGER.info("Action executed")
                        action.exec(entity, pos)
                        state = ErrandState.COMPLETED
                    } else {
                        tickToExecCount++
                    }
                }
                ErrandState.COMPLETED -> {
                    if (action.eval(entity, pos) > 0) {
                        LOGGER.info("Errand concluded")
                        resetState()
                        entity.getErrandsManager().pop()
                    } else {
                        // do stuff
                    }
                }
            }
            if (entity.navigation.isIdle) {
                path = null
            }
        }
    }

    private fun resetState() {
        tickToTestCount = 0
        tickToExecCount = 0
        state = ErrandState.PENDING
        errand = null
        path = null
    }

    private enum class ErrandState {
        PENDING, TESTED, COMPLETED
    }
}
