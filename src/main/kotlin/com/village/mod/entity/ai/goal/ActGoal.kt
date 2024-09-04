package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.util.Finder
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import java.util.EnumSet

class ActGoal(
    private val entity: CustomVillagerEntity,
) : Goal() {
    private val world = entity.world

    private var tickToTestCount = 0
    private var tickToExecCount = 0

    private var ticksWithoutPath = 0

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
                    path = entity.navigation.findPathTo(c.x, c.y, c.z, 0)
                } ?: run {
                    entity.target?.let { t ->
                        path = entity.navigation.findPathTo(t, 0)
                    } ?: return true
                }
            }
        }
        return path != null
    }

    override fun shouldContinue(): Boolean = false

    override fun shouldRunEveryTick() = true

    override fun tick() {
        errand?.let { (cid, pos) ->
            // LOGGER.info("State: {}, Ticks: {}|{}", state, tickToTestCount, tickToExecCount)
            val action = Action.get(cid)
            val distance =
                entity.target?.let { target ->
                    val distance = entity.squaredDistanceTo(target)
                    if (action.shouldLook(distance)) {
                        entity.getLookControl().lookAt(target)
                    }
                    // handle special cases like flee
                    if (entity.isAttacking() && cid != Action.Type.FLEE) {
                        if (target.isAlive) {
                            val npath = entity.navigation.findPathTo(target, 1)
                            if (npath != null) {
                                path = npath
                            }
                            distance
                        } else {
                            return
                        }
                    } else {
                        null
                    }
                } ?: run {
                    pos?.let {
                        val point = it.toCenterPos()
                        var distance = entity.squaredDistanceTo(point)
                        if (action.shouldLook(distance)) {
                            entity.getLookControl().lookAt(point)
                        }
                        distance
                    } ?: 0.0
                }

            if (action.shouldMove(distance)) {
                path?.let {
                    entity.getUp()
                    Finder.findDoorBlock(entity.world, path!!)?.let {
                        entity.getErrandsManager().add(it)
                    }
                    entity.navigation.startMovingAlong(
                        path,
                        if (entity.isUsingItem) {
                            0.5
                        } else {
                            action.speedModifier
                        },
                    )
                }

                if (entity.navigation.isIdle) {
                    ticksWithoutPath++
                    LOGGER.info("> No available PATH")
                } else {
                    ticksWithoutPath = 0
                }

                if (ticksWithoutPath >= 20) {
                    // unable to find path
                    LOGGER.info("> No path available, popping it")
                    entity.getErrandsManager().pop()
                    ticksWithoutPath = 0
                    return
                }

                // lame implementation of Door openning
            } else {
                tickToTestCount++
            }

            when (state) {
                ErrandState.PENDING -> {
                    if (action.shouldTest(tickToTestCount)) {
                        // make 2 skips exec ticks
                        when (action.test(entity, pos)) {
                            1.toByte() -> {
                                LOGGER.info("> Test passed")
                                state = ErrandState.TESTED
                            }
                            else -> {
                                LOGGER.info("> Test failed")
                                resetState()
                                entity.getErrandsManager().pop()
                                return
                            }
                        }
                    } else {
                        // tickToTestCount++
                    }
                }
                ErrandState.TESTED -> {
                    if (action.shouldExec(tickToExecCount)) {
                        LOGGER.info("> Action executed")
                        action.exec(entity, pos)
                        state = ErrandState.COMPLETED
                    } else {
                        tickToExecCount++
                    }
                }
                ErrandState.COMPLETED -> {
                    when (action.eval(entity, pos)) {
                        1.toByte() -> {
                            LOGGER.info("> Errand concluded")
                            resetState()
                            entity.getErrandsManager().pop()
                        }
                        2.toByte() -> {
                            entity.getErrandsManager().pop()
                            resetState()
                            action.redo(entity, pos)
                        }
                        else -> {
                            // LOGGER.info("> Errand finishing...")
                        }
                    }
                }
            }
        }
    }

    private fun resetState() {
        entity.navigation.stop()
        tickToTestCount = 0
        tickToExecCount = 0
        state = ErrandState.PENDING
        errand = null
        path = null
    }

    private enum class ErrandState {
        PENDING,
        TESTED,
        COMPLETED,
    }
}
