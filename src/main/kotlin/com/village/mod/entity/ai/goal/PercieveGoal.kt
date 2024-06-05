package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.mob.Monster
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.math.Box
import java.util.EnumSet

class PercieveGoal(private val entity: CustomVillagerEntity) : Goal() {
    private var timeWithoutVisibility: Int = 0
    private var tickCooldown: Int = 0
    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    override fun canStart(): Boolean {
        if (entity.isSleeping()) {
            return false
        }
        if (entity.random.nextInt(5) != 0) {
            return false
        }
        return true
    }

    override fun shouldContinue(): Boolean {
        val liv = this.entity.getTarget()
        if (liv != null) {
            if (!this.entity.canTarget(liv)) {
                LOGGER.info("I AM INVALID")
                this.entity.setTarget(null)
                return false
            }
            if (this.entity.squaredDistanceTo(liv) > 2048.0f) {
                this.entity.setTarget(null)
                LOGGER.info("I AM DISTANT")
                return false
            }
        }
        return false
    }

    private fun getSearchBox(distance: Double): Box {
        return entity.boundingBox.expand(distance, 4.0, distance)
    }

    private fun getNearbyEntities() {
        LOGGER.info("WASTE OF")
        val entities = entity.getWorld().getOtherEntities(entity, this.getSearchBox(12.0))
        // TODO: use entities types and positions to make entity decide what to do
        val hostiles = entities.filter { it is Monster }
        if (hostiles.isNotEmpty()) {
            hostiles.filter { entity.getVisibilityCache().canSee(it) }
                .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
                    LOGGER.info("sensed monster - {}", it)
                    entity.setTarget(it as LivingEntity)
                    // target = it //entity.getTarget()
                    entity.setAttacking(true)
                    return
                }
        }
        // if (entity.random.nextInt(32) == 0) {
        //    val friendly = entities.filter { it is CustomVillagerEntity }
        //    if (friendly.isNotEmpty()) {
        //        friendly.filter { entity.getVisibilityCache().canSee(it) && (it as CustomVillagerEntity).target == null && it.errand.isEmpty() }

        //            .minByOrNull { entity.blockPos.getSquaredDistance(it.pos) }?.let {
        //                LOGGER.info("sensed frien")
        //                if (target != it) {
        //                    (it as CustomVillagerEntity).setTarget(entity)
        //                    entity.setTarget(it as LivingEntity)
        //                    target = it//entity.getTarget()
        //                }
        //            }
        //    }
        // }
    }

    override fun start() {
        if (tickCooldown > 5) {
            this.getNearbyEntities()
            tickCooldown = 0
        }
        LOGGER.info("JUST FLOATING")
        tickCooldown++
        if (entity.isTouchingWater() && entity.getFluidHeight(FluidTags.WATER) > entity.getSwimHeight() || entity.isInLava()) {
            if (entity.getRandom().nextFloat() < 0.6f) {
                entity.getJumpControl().setActive()
            }
        }
    }
}
