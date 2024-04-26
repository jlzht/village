package com.village.mod.entity.ai.goal

import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Fisherman
import com.village.mod.village.villager.State
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.util.Hand
import java.util.EnumSet

class FishingGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var fishCountdown: Int = 0

    init {
        controls = EnumSet.of(Control.MOVE, Control.LOOK)
    }

    override fun canStart(): Boolean {
        if (entity.isState(State.CAST)) {
            if (entity.peekTargetBlock() != null) {
                return true
            }
        }
        return false
    }

    override fun shouldContinue(): Boolean {
        fishCountdown++
        if (fishCountdown > 100) {
            return false
        }
        return true
    }

    override fun stop() {
        (entity.getProfession() as Fisherman).TryCatch(entity)
        entity.popTargetBlock()
        entity.setState(State.IDLE)
        fishCountdown = 0
    }

    override fun start() {
        val fishHook = (entity.getProfession() as Fisherman).getFishHook()
        if (fishHook == null) {
            (entity.getProfession() as Fisherman).TryFish(entity)
            (entity as LivingEntity).swingHand(Hand.MAIN_HAND)
        }
    }
}
