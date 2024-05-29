package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
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
        return entity.getTarget() == null || entity.isAttacking()
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
        } else if (++this.timeWithoutVisibility > 150) {
            return false
        }
        this.entity.setTarget(target)
        return false
    }

    private fun getSearchBox(distance: Double): Box {
        return entity.boundingBox.expand(distance, 4.0, distance)
    }

    private fun getNearbyEntities() {
        val entities = entity.getWorld().getOtherEntities(entity, this.getSearchBox(18.0))
        // TODO: use entities types and positions to make entity decide what to do
        val hostiles = entities.filter { it is Monster }
        if (hostiles.isNotEmpty()) {
            hostiles.filter { entity.getVisibilityCache().canSee(it) }
                .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
                    LOGGER.info("sensed monster - {}", it)
                    entity.setTarget(it as LivingEntity)
                    entity.setAttacking(true)
                    return
                }
        }
        if (entity.random.nextInt(32) == 0) {
            val friendly = entities.filter { it is CustomVillagerEntity }
            if (friendly.isNotEmpty()) {
                friendly.filter { entity.getVisibilityCache().canSee(it) && (it as CustomVillagerEntity).target == null && it.errand.isEmpty() }
                    .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
                        LOGGER.info("sensed frien")
                        if (target != it) {
                            (it as CustomVillagerEntity).setTarget(entity)
                            entity.setTarget(it as LivingEntity)
                        }
                    }
            }
        }
    }

    override fun start() {
        this.getNearbyEntities()
    }
}
