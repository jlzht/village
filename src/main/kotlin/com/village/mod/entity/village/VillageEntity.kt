package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.entity.ai.goal.*
import com.village.mod.screen.TradingScreenHandler

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.goal.LookAtEntityGoal
import net.minecraft.entity.ai.goal.SwimGoal
import net.minecraft.entity.ai.goal.WanderAroundGoal
import net.minecraft.entity.ai.pathing.MobNavigation
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class VigerEntity(entityType: EntityType<out VigerEntity>?, world: World?) :
    PathAwareEntity(
        entityType,
        world,
    ),
    ExtendedScreenHandlerFactory {
    public var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(8, ItemStack.EMPTY)

    protected val SITTING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.35f)
    protected val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.95f)

    var actionTime: Long = 0
    var checkupTime: Long = 0

    init {
        var entNav: MobNavigation = this.getNavigation() as MobNavigation
        entNav.setCanSwim(true)
        entNav.canEnterOpenDoors()
        entNav.setCanPathThroughDoors(true)
        setCanPickUpLoot(true)
    }

    fun isInvFull(): Boolean {
        for (i in 0..7) {
            if (inventory[i] == ItemStack.EMPTY) {
                return false
            }
        }
        return true
    }

    fun addToInventory(itemStack: ItemStack): Int {
        for (i in 0..7) {
            if (inventory[i] == ItemStack.EMPTY) {
                inventory[i] = itemStack.copy()
                return inventory[i].getCount()
            }
            if (inventory[i].item == itemStack.item) {
                if (inventory[i].maxCount > itemStack.getCount()) {
                    LOGGER.info("I will increment only {}", itemStack.getCount())
                    inventory[i].increment(itemStack.getCount())
                    return itemStack.getCount()
                }
                var nCount: Int = (inventory[i].maxCount - inventory[i].getCount()) // 63
                if (nCount > 0) {
                    LOGGER.info("x - I will increment only {}", nCount)
                    return itemStack.getCount() - nCount
                }
            }
        }
        return 0
    }

    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return if (pose == EntityPose.SLEEPING) {
            SLEEPING_DIMENSIONS
        } else if (pose == EntityPose.SITTING) {
            SITTING_DIMENSIONS
        } else {
            STANDING_DIMENSIONS
        }
    }

    override fun initGoals() {
        goalSelector.add(0, SwimGoal(this))
        goalSelector.add(4, WanderAroundGoal(this, 1.0))
        goalSelector.add(7, SitOnSurfaceGoal(this))
        goalSelector.add(7, LookAtEntityGoal(this, VigerEntity::class.java, 6.0f))
        goalSelector.add(9, CheckNearbyItemsGoal(this))
    }

    companion object {
        fun createMobAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0)
        }
    }

    override fun getUnscaledRidingOffset(vehicle: Entity): Float {
        return -0.6f
    }

    override fun writeScreenOpeningData(
        player: ServerPlayerEntity,
        buf: PacketByteBuf,
    ) {
        buf.writeBlockPos(this.getBlockPos())
    }

    override fun createMenu(
        syncId: Int,
        playerInventory: PlayerInventory,
        player: PlayerEntity,
    ): ScreenHandler? {
        return TradingScreenHandler(syncId, playerInventory)
    }

    override fun interactMob(
        player: PlayerEntity,
        hand: Hand,
    ): ActionResult {
        val itemStack: ItemStack = player.getStackInHand(hand)
        if (!itemStack.isOf(Items.VILLAGER_SPAWN_EGG) && isAlive && !isSleeping) {
            if (!world.isClient) {
                val optionalInt = player.openHandledScreen(this)
            }
            return ActionResult.SUCCESS.also { it }
        }
        return super.interactMob(player, hand)
    }

    fun dropInventoryItems(
        world: World,
        pos: BlockPos,
    ) {
        LOGGER.info("- {}, {}", this.inventory[0].item, this.inventory[0].count)
        LOGGER.info("- {}, {}", this.inventory[1].item, this.inventory[1].count)
        LOGGER.info("- {}, {}", this.inventory[2].item, this.inventory[2].count)
        LOGGER.info("- {}, {}", this.inventory[3].item, this.inventory[3].count)
        LOGGER.info("- {}, {}", this.inventory[4].item, this.inventory[4].count)
        LOGGER.info("- {}, {}", this.inventory[5].item, this.inventory[5].count)
        LOGGER.info("- {}, {}", this.inventory[6].item, this.inventory[6].count)
        LOGGER.info("- {}, {}", this.inventory[7].item, this.inventory[7].count)
        ItemScatterer.spawn(world, pos, inventory)
    }

    public fun isRiding(): Boolean {
        return this.vehicle is BoatEntity
    }

    public fun isSitting(): Boolean {
        return this.pose == EntityPose.SITTING
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Viger {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        var entity: Entity? = damageSource.getAttacker()
        dropInventoryItems(world, this.getBlockPos())
        if (entity != null) {
            val world = this.world
            if (world !is ServerWorld) {
                return
            }
        }
    }
}
