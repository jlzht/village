package com.village.mod.entity.ai.goal

import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Combatant
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.EnumSet

// This goal only manage interactions with entities
class PercieveGoal(
    private val entity: CustomVillagerEntity,
) : Goal() {
    private var timeWithoutVisibility: Int = 0
    private var tickCooldown: Int = 0
    private var ticksWithoutTarget: Int = 0

    init {
        this.setControls(EnumSet.of(Goal.Control.TARGET))
    }

    // TODO: add special behavior for lava
    override fun canStart(): Boolean {
        if (entity.isSleeping()) return false
        entity.target?.let { target ->
            entity.getRecentDamageSource()?.let { damageSource ->
                val attacker = damageSource.getAttacker()
                if (attacker is LivingEntity) {
                    if (entity.getProfession() is Combatant) {
                        if (!(target is PlayerEntity && target.isCreative)) {
                            entity.setAttacking(true)
                        }
                    }
                    entity.getUp()
                    entity.setFighting(true)
                    entity.setTarget(attacker)
                    return false
                }
            }
            if (entity.squaredDistanceTo(target) > 32 * 32) {
                entity.target = null
                return false
            }
            return !target.isAlive
        } ?: run {
            return true
        }
    }

    override fun shouldContinue(): Boolean = false

    override fun start() {
        entity.setFighting(false)
        entity.setAttacking(false)
        if (tickCooldown > 5) {
            tickCooldown = 0
            this.getNearbyEntities()
        }
        tickCooldown++

        if (entity.isFighting()) {
            entity.getUp()
        }
    }

    private fun getSearchBox(distance: Double): Box = entity.boundingBox.expand(distance, 4.0, distance)

    private fun getNearbyEntities() {
        entity
            .getWorld()
            .getOtherEntities(entity, this.getSearchBox(16.0))
            .filter { entity.getVisibilityCache().canSee(it) }
            .let { entities ->

                var direction: Vec3d = Vec3d.ZERO
                entities.filterIsInstance<LivingEntity>().forEach { target ->
                    direction =
                        direction.add(
                            if (target is Monster) {
                                entity.pos.add(target.pos)
                            } else {
                                entity.pos.subtract(target.pos)
                            },
                        )
                }
                entity.setFighting(entities.any { it is Monster })

                entities
                    .filterIsInstance<LivingEntity>()
                    .minByOrNull { target ->
                        // more tests needed
                        direction.normalize().squaredDistanceTo(target.pos.normalize())
                    }?.let {
                        when (it) {
                            is PlayerEntity -> {
                                // TODO: check reputation for set attacking
                                if (!it.isCreative) entity.setTarget(it)
                            }
                            is HostileEntity -> {
                                entity.setAttacking(true)
                                entity.setTarget(it)
                            }
                            else -> entity.setTarget(it)
                        }
                        if (entity.isFighting()) return
                        if (entity.random.nextInt(5) != 0) return // no danger, so it tries to pickup nearby items
                    }

                if (!entity.getErrandsManager().has(Action.Type.PICK)) {
                    entities
                        .filterIsInstance<ItemEntity>()
                        .filter { it.getItemAge() > 20 }
                        .filter { entity.visibilityCache.canSee(it) }
                        .filter { entity.canGather(it.stack) }
                        .sortedBy { entity.squaredDistanceTo(it) }
                        .firstOrNull()
                        ?.let { item ->
                            entity.getErrandsManager().add(Action.Type.PICK, item.blockPos.up(1))
                        }
                }
            }
    }
}
