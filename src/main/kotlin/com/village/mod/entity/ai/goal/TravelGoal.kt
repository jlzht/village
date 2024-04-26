//package com.village.mod.entity.ai.goal
//
//import com.village.mod.entity.village.CustomVillagerEntity
//import com.village.mod.village.villager.State
//import net.minecraft.block.Block
//import net.minecraft.block.Blocks
//import net.minecraft.entity.ai.goal.Goal
//import net.minecraft.util.Hand
//import java.util.EnumSet
//import com.village.mod.LOGGER
//
//class TravelGoal(private val entity: CustomVillagerEntity) : Goal() {
//    init {
//        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))
//    }
//
//    override fun canStart(): Boolean {
//        if (entity.isState(State.TRAVEL) && !entity.isTargetBlockEmpty()) {
//            val block = entity.peekTargetBlock()
//            if (entity.squaredDistanceTo(block.toCenterPos()) < 16)
//            return entity.world.getBlockState(block).isOf(Blocks.BELL)
//        }
//        LOGGER.info("CANSTART")
//        return false
//    }
//
//    override fun shouldContinue(): Boolean {
//        return false
//    }
//
//    override fun stop() {
//    }
//
//    override fun start() {
//        entity.popTargetBlock()
//        entity.removeTargetGoal()
//    }
//}
