package com.village.mod.entity.ai.goal

import com.village.mod.LOGGER
import com.village.mod.entity.village.VigerEntity

import net.minecraft.entity.ItemEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.item.ItemStack
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

import java.util.EnumSet

class CheckNearbyItemsGoal(private val entity: VigerEntity) : Goal() {
    private var trackedItem: ItemEntity? = null
    private var itemPath: Path? = null

    init {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK))
    }

    private val itemEntityPredicate = EntityPredicates.VALID_ENTITY

    override fun canStart(): Boolean {
        if (entity.isRiding() || entity.isSitting()) {
            return false
        }
        if (entity.isInvFull()) {
            return false
        }
        val searchCenter = entity.getBlockPos()
        val nearbyItems = entity.world.getEntitiesByClass(ItemEntity::class.java, Box(searchCenter).expand(3.0), itemEntityPredicate)
        for (itemEntity in nearbyItems) {
            val itemPos: Vec3d = itemEntity.getPos()
            trackedItem = itemEntity
            itemPath = entity.getNavigation().findPathTo(itemPos.x, itemPos.y, itemPos.z, 0)
            LOGGER.info("ITEM:{}", this.trackedItem!!.name.string)
            return true
        }
        return false
    }

    override fun start() {
        if (trackedItem != null && itemPath != null) {
            entity.getNavigation().startMovingAlong(itemPath, 1.0)
        }
    }

    override fun shouldContinue(): Boolean {
        if (entity.getNavigation().isIdle()) {
            val item = this.trackedItem!!
            val itemStack: ItemStack = item.getStack()
            val it: Int = entity.addToInventory(itemStack)
            entity.sendPickup(item, it)
            itemStack.decrement(it)
            if (itemStack.isEmpty()) {
                item.discard()
            }
            return false
        }
        return true
    }

    override fun stop() {
        this.trackedItem = null
    }
}
