package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
import com.village.mod.village.villager.State
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityPose
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import java.util.EnumSet

class GotoGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world
    private var callback: (() -> Unit)? = null
    private var desiredPos: Vec3d? = null
    private var errand: Errand? = null
    private var speed: Double = 1.0
    private var radius: Float = 0.5f
    private var shouldLookAt = true
    private var ticksInField = 0
    private var ticksToConsume = 5

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.errand.isEmpty()) return false
        // TODO: add a method to check errand priority by making a weighted decision of which errand to pickup instead of get the first
        this.errand = entity.errand.peek()
        errand?.let { peeked ->
            desiredPos = peeked.pos.toCenterPos()
            this.checkAction(entity.errand.peek())
            return true
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        if (ticksInField >= ticksToConsume) {
            ticksInField = 0
            callback?.invoke()
            entity.errand.pop(errand!!)
            return false
        }
        return true
    }
    override fun shouldRunEveryTick() = true

    override fun tick() {
        desiredPos?.let { pos ->
            if (entity.squaredDistanceTo(pos) < radius) {
                if (shouldLookAt) {
                    entity.lookControl.lookAt(pos.x, pos.y + 0.5f, pos.z, 30.0f, 30.0f)
                }
                ticksInField++
            } else {
                entity.navigation.startMovingTo(pos.x, pos.y, pos.z, speed)
            }
        }
    }

    private fun setAction(radius: Float, callback: () -> Unit, speed: Double = 1.0, shouldLookAt: Boolean = true) {
        this.radius = radius
        this.callback = callback
        this.speed = speed
        this.shouldLookAt = shouldLookAt
    }

    private fun checkAction(errand: Errand) {
        this.errand = errand
        when (errand.action) {
            Action.TILL -> setAction(2.4f, {
                entity.world.setBlockState(errand.pos, Blocks.FARMLAND.getDefaultState(), Block.NOTIFY_LISTENERS)
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.PLANT -> setAction(2.4f, {
                entity.world.setBlockState(errand.pos.up(), Blocks.WHEAT.getDefaultState(), Block.NOTIFY_LISTENERS)
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.FISH -> setAction(8.0f, {
                entity.getNavigation().stop()
                entity.getProfession()?.castAction(entity)
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.MOVE -> setAction(4.0f, {}, 1.5, false)
            Action.SLEEP -> setAction(1.8f, {
                entity.sleep(errand.pos)
                entity.state.set(State.SLEEP)
            })
            Action.SIT -> setAction(0.5f, {
                val destPos: Vec3d = errand.pos.toCenterPos()
                entity.setPosition(destPos.getX(), destPos.getY(), destPos.getZ())
                entity.setPose(EntityPose.SITTING)
            })
            else -> { LOGGER.info("I WILL DO NOTHING!") }
        }
    }
}
