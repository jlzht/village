package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.mob.Monster
import net.minecraft.util.math.Box
import java.util.EnumSet

class PercieveGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var timeWithoutVisibility: Int = 0
    private var tickCooldown: Int = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        if (entity.isSleeping()) return false
        if (entity.random.nextInt(5) != 0) return false
        return entity.target?.isAlive != true
    }

    override fun shouldContinue(): Boolean = false

    private fun getSearchBox(distance: Double): Box {
        return entity.boundingBox.expand(distance, 4.0, distance)
    }

    private fun getNearbyEntities() {
        val entities = entity.getWorld().getOtherEntities(entity, this.getSearchBox(12.0))
        // TODO: use entities types and positions to make entity decide what to do
        val hostiles = entities.filter { it is Monster }
        if (hostiles.isNotEmpty()) {
            hostiles.filter { entity.getVisibilityCache().canSee(it) }
                .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
                    LOGGER.info("sensed monster - {}", it)
                    val k = (it as LivingEntity)
                    entity.setTarget(k)
                    entity.setAttacking(true)
                    return
                }
        } else {
            entity.setTarget(null)
            entity.setAttacking(false)
        }
    }

    override fun start() {
        if (tickCooldown > 2) {
            this.getNearbyEntities()
            tickCooldown = 0
        }
        tickCooldown++
    }
}
