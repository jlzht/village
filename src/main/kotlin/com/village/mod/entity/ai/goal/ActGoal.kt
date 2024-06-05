package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.village.villager.Action
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import java.util.EnumSet

class ActGoal(private val entity: CustomVillagerEntity) : Goal() {
    private val world = entity.world
    private var callback: (() -> Unit)? = null
    private var desiredPos: Vec3d? = null
    private var errand: Errand? = null
    private var speed: Double = 1.0
    private var radius: Float = 0.5f
    private var shouldLookAt = true
    private var ticksInField = 0
    private var ticksToFade = 0
    private var ticksToConsume = 5
    private var lastPath: Path? = null

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    override fun canStart(): Boolean {
        if (entity.errand.isEmpty()) return false
        if (entity.isSleeping()) return false
        if (errand == entity.errand.peek() && errand != null) return false
        entity.errand.peek().let { peeked ->
            desiredPos = peeked.pos.toCenterPos()
            LOGGER.info("TIME TO RUN! {}", desiredPos)
            this.checkAction(entity.errand.peek())
            return true
        }
    }

    override fun shouldContinue(): Boolean {
        this.canStart()
        if (ticksToFade >= 600) {
            this.errand?.let {
                LOGGER.info("popping - {}", it)
                entity.errand.pop(it)
                errand = null
                ticksToFade = 0
            }
        }
        if (ticksInField >= ticksToConsume) {
            ticksInField = 0
            callback?.invoke()
            this.errand?.let {
                LOGGER.info("popping - {}", it)
                entity.errand.pop(it)
                errand = null
            }
            return false
        }
        return true
    }
    override fun shouldRunEveryTick() = true

    override fun tick() {
        LOGGER.info("| {}", errand)
        desiredPos?.let { pos ->
            if (errand == null) return
            ticksToFade++
            this.entity.getNavigation().findPathTo(errand?.pos, 0)?.let { path ->
                if (errand?.action != Action.SIT && errand?.action != Action.CLOSE && errand?.action != Action.OPEN) {
                    val kk = path.getCurrentNode().getBlockPos()
                    if (!kk.equals(errand?.pos)) {
                        val blockState = world.getBlockState(kk)
                        if (blockState.getBlock() is DoorBlock) {
                            if (!blockState.get(DoorBlock.OPEN)) {
                                val j = Errand(kk, Action.OPEN)
                                if (!entity.errand.contains(j) && entity.squaredDistanceTo(errand?.pos?.toCenterPos()) > 4.0f) {
                                    entity.errand.push(j)
                                    LOGGER.info("OPEN")
                                }
                            } else {
                                val a = Errand(kk, Action.CLOSE)
                                val direction = blockState.get(DoorBlock.FACING)
                                val r = entity.blockPos.getSquaredDistance(kk.offset(direction).toCenterPos())
                                val l = entity.blockPos.getSquaredDistance(kk.offset(direction.getOpposite()).toCenterPos())
                                entity.errand.push(Errand(if (r > l) { kk.offset(direction) } else { kk.offset(direction.getOpposite()) }, Action.SIT))

                                if (!entity.errand.contains(a)) {
                                    entity.errand.push(a)
                                    LOGGER.info("CLOSE")
                                }
                            }
                        }
                    }
                }
                if (errand?.action != Action.CLOSE) {
                    entity.navigation.startMovingAlong(path, 1.0)
                }
            }
            if (entity.squaredDistanceTo(pos) < radius) {
                if (shouldLookAt) {
                    entity.lookControl.lookAt(pos.x, pos.y + 0.5f, pos.z, 30.0f, 30.0f)
                }
                ticksInField++
            }
        }
    }

    private fun setAction(radius: Float, callback: () -> Unit, speed: Double = 1.0, shouldLookAt: Boolean = true) {
        this.radius = radius
        this.callback = callback
        this.speed = speed
        this.shouldLookAt = shouldLookAt
        ticksInField = 0
    }

    private fun checkAction(errand: Errand) {
        this.errand = errand
        when (errand.action) {
            Action.TILL -> setAction(2.4f, {
                entity.world.setBlockState(errand.pos, Blocks.FARMLAND.getDefaultState(), Block.NOTIFY_LISTENERS)
                ticksToConsume = 10
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.PLANT -> setAction(2.4f, {
                entity.world.setBlockState(errand.pos.up(), Blocks.WHEAT.getDefaultState(), Block.NOTIFY_LISTENERS)
                ticksToConsume = 15
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.OPEN -> setAction(1.5f, {
                val k = entity.world.getBlockState(errand.pos)
                (k.getBlock() as DoorBlock).setOpen(entity, entity.getWorld(), k, errand.pos, true)
                entity.swingHand(Hand.MAIN_HAND)
                val direction = k.get(DoorBlock.FACING)
                val r = entity.blockPos.getSquaredDistance(errand.pos.offset(direction).toCenterPos())
                val l = entity.blockPos.getSquaredDistance(errand.pos.offset(direction.getOpposite()).toCenterPos())
                entity.errand.push(Errand(errand.pos, Action.CLOSE))
                entity.errand.push(Errand(if (r > l) { errand.pos.offset(direction) } else { errand.pos.offset(direction.getOpposite()) }, Action.SIT))
                LOGGER.info("GOT OPEN")
                ticksToConsume = 5
            })
            Action.CLOSE -> setAction(1.25f, {
                val k = entity.world.getBlockState(errand.pos)
                (k.getBlock() as DoorBlock).setOpen(entity, entity.getWorld(), k, errand.pos, false)
                entity.swingHand(Hand.MAIN_HAND)
                LOGGER.info("GOT CLOSE")
                ticksToConsume = 5
            })
            Action.BREAK -> setAction(2.4f, {
                entity.world.breakBlock(errand.pos, true, entity)
                ticksToConsume = 20
            })

            Action.FISH -> setAction(8.0f, {
                entity.getNavigation().stop()
                entity.getProfession()?.doWork()
                entity.swingHand(Hand.MAIN_HAND)
            })
            Action.MOVE -> setAction(8.0f, {
                ticksToConsume = 10
            }, 1.5, false)
            Action.SLEEP -> setAction(2.8f, {
                entity.sleep(errand.pos)
            })
            Action.SIT -> setAction(1.15f, {
                ticksToConsume = 15
                // entity.sit(errand.pos)
            })
            else -> { LOGGER.info("I WILL DO NOTHING!") }
        }
    }
}
