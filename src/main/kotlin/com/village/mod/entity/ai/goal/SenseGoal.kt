package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import com.village.mod.village.villager.Task
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.mob.Monster
import net.minecraft.util.math.Box
import java.util.EnumSet

class SenseGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var target: LivingEntity? = null
    private var timeWithoutVisibility: Int = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        if (entity.state.isAt(State.SLEEP)) {
            return false
        }
        if (entity.random.nextInt(10) != 0) {
            return false
        }
        this.getNearbyEntities()
        return target != null
    }
    override fun shouldContinue(): Boolean {
        if (entity.state.isAt(State.SLEEP)) {
            return false
        }
        if (target == null) {
            return false
        }
        if (!this.entity.canTarget(target)) {
            return false
        }
        if (this.entity.squaredDistanceTo(target) > 1024.0f) {
            return false
        }
        if (this.entity.getVisibilityCache().canSee(target)) {
            this.timeWithoutVisibility = 0
        } else if (++this.timeWithoutVisibility > 20 * 12) {
            return false
        }
        this.entity.setTarget(target)
        return true
    }

    private fun getSearchBox(distance: Double): Box {
        return entity.boundingBox.expand(distance, 4.0, distance)
    }

    private fun getNearbyEntities() {
        val entities = entity.getWorld().getOtherEntities(entity, this.getSearchBox(10.0))
        // TODO: use entities types and positions to make entity decide what to do
        val hostiles = entities.filter { it is Monster }
        if (hostiles.isNotEmpty()) {
            hostiles.filter { entity.getVisibilityCache().canSee(it) }
                .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
                    entity.task.set(Task.ALERT)
                    target = it as LivingEntity
                }
        }
    }

    override fun start() {
    }
    override fun stop() {
        this.entity.setTarget(null)
        this.target = null
    }
}
