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

class GotoGoal(private val entity: CustomVillagerEntity) : Goal() {
    data class Attributes(val speed: Double, val radius: Float, val state: State)
    private var callback: (() -> Unit)? = null
    private val world = entity.world
    private var blockPos: BlockPos? = null
    private var performer = Attributes(1.0, 3.0f, State.NONE)
    private var inFieldTick = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (!entity.isTargetBlockEmpty()) {
            this.blockPos = entity.peekTargetBlock()
            getPerformer(blockPos!!)
            return true //entity.isState(performer.component3())
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        if (inFieldTick > 10) {
            inFieldTick = 0
            callback?.invoke()
            entity.popTargetBlock()
            return false
        }
        return true
    }
    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    override fun tick() {
        // TODO:  Paths
        if (blockPos != null) {
            val desiredPos = blockPos!!.toCenterPos()
            entity.lookControl.lookAt(desiredPos.x, desiredPos.y + 0.5f, desiredPos.z, 30.0f, 30.0f)
            entity.getNavigation().startMovingTo(desiredPos.x, desiredPos.y, desiredPos.z, performer.component1())
            if (entity.squaredDistanceTo(desiredPos) < performer.component2()) {
                inFieldTick++
            }
        }
    }

    override fun stop() {
    }
    private fun getPerformer(blockPos: BlockPos) {
        val pos = world.getBlockState(blockPos)
        callback = null
        when {
            pos.isOf(Blocks.GRASS_BLOCK) || pos.isOf(Blocks.DIRT) -> {
                performer = Attributes(1.2, 2.4f, State.WORK)
                callback = {
                    LOGGER.info("I WILL POP IT UP GRASSBLOCK")
                    val blo = this.entity.peekTargetBlock()
                    this.entity.world.setBlockState(blo, Blocks.FARMLAND.defaultState, Block.NOTIFY_LISTENERS)
                    this.entity.swingHand(Hand.MAIN_HAND, true)
                }
            }
            pos.isOf(Blocks.WATER) -> {
                performer = Attributes(1.1, 8.0f, State.WORK)
            }
            pos.isOf(Blocks.BELL) -> {
                performer = Attributes(1.5, 20.0f, State.TRAVEL)
                callback = {
                    LOGGER.info("I WILL POP IT UP BELL")
                }
            }
            pos.isIn(BlockTags.SLABS) -> performer = Attributes(1.2, 0.5f, State.IDLE)
        }
    }
}
