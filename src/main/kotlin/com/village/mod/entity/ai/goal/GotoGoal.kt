package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.math.BlockPos
import net.minecraft.registry.tag.BlockTags
import java.util.EnumSet
import com.village.mod.LOGGER
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.util.Hand
import com.village.mod.BlockAction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

class GotoGoal(private val entity: CustomVillagerEntity) : Goal() {
    data class Attributes(val speed: Double, val radius: Float, val state: State)
    private var callback: (() -> Unit)? = null
    private val world = entity.world
    private var action: BlockAction? = null
    private var performer = Attributes(1.0, 3.0f, State.NONE)
    private var inFieldTick = 0
    private var desiredPos: Vec3d? = null

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (!entity.isTargetBlockEmpty()) {
            this.action = entity.getTargetBlock()
            if (this.action != null) {
                val action = this.action!!
                desiredPos = action.pos.toCenterPos()
                callback = null
                when (action.block) {
                    Blocks.FARMLAND -> {
                        performer = Attributes(1.2, 2.4f, State.WORK)
                        callback = {
                            this.entity.world.setBlockState(action.pos, Blocks.FARMLAND.getDefaultState(), Block.NOTIFY_LISTENERS)
                            this.entity.swingHand(Hand.MAIN_HAND)
                        }
                    }
                    Blocks.WHEAT -> {
                        performer = Attributes(1.2, 2.4f, State.WORK)
                        callback = {
                            this.entity.world.setBlockState(action.pos, Blocks.WHEAT.getDefaultState(), Block.NOTIFY_LISTENERS)
                            this.entity.swingHand(Hand.MAIN_HAND)
                        }

                    }
                    Blocks.WATER -> {
                        LOGGER.info("ME HERE!")
                        performer = Attributes(1.0, 8.0f, State.WORK)
                        callback = {
                            this.entity.getNavigation().stop()
                            this.entity.getProfession().castAction(entity)
                            this.entity.swingHand(Hand.MAIN_HAND)
                        }
                    }
                    Blocks.BELL -> {
                        performer = Attributes(1.5, 20.0f, State.TRAVEL)
                        callback = {
                        }
                    }
                    Blocks.OAK_SLAB -> performer = Attributes(1.2, 0.5f, State.IDLE)
                }
                return true
            }
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        if (inFieldTick >= 10) { // TODO: add wait ticks to perfomer
            inFieldTick = 0
            callback?.invoke()
            entity.delTargetBlock(this.action!!)
            return false
        }
        return true
    }
    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    override fun tick() {
        if (this.desiredPos != null) {
            // TODO: fix this annoyance
            val desiredPos = this.desiredPos!!
            if (entity.squaredDistanceTo(desiredPos) < performer.component2()) {
                entity.lookControl.lookAt(desiredPos.x, desiredPos.y + 0.5f, desiredPos.z, 30.0f, 30.0f)
                inFieldTick++
            } else {
                entity.getNavigation().startMovingTo(desiredPos.x, desiredPos.y, desiredPos.z, performer.component1())
            }
        }
    }

    override fun stop() {}
}
