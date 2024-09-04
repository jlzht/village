package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.entity.ai.goal.ActGoal
import com.village.mod.entity.ai.goal.PercieveGoal
import com.village.mod.entity.ai.goal.PlanGoal
import com.village.mod.entity.ai.goal.ReactGoal
import com.village.mod.entity.ai.pathing.VillagerNavigation
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.world.SettlementManager
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityData
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.ai.pathing.EntityNavigation
import net.minecraft.entity.ai.pathing.MobNavigation
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.UseAction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LocalDifficulty
import net.minecraft.world.ServerWorldAccess
import net.minecraft.world.World

data class VillagerData(
    var key: Int = -1,
    var sid: Int = -1,
    var dim: String = "i",
)

// TODO:
// - rename CustomVillagerEntity to AbstractVillagerEntity
// - make it suitable for preferred hand selection
class CustomVillagerEntity(
    entityType: EntityType<out CustomVillagerEntity>,
    world: World?,
) : PathAwareEntity(entityType, world),
    ExtendedScreenHandlerFactory {
    private val errandsManager = ErrandManager(this)

    fun getErrandsManager(): ErrandManager = errandsManager

    val inventory: VillagerInventory = VillagerInventory(this)
    private lateinit var profession: Profession

    val data = VillagerData()

    // villagerData key - id , village id, village dim
    // var key: Int = -1
    // var villageKey: Int = -1

    init {
        this.getNavigation().setCanSwim(false)
        (this.getNavigation() as MobNavigation).setCanPathThroughDoors(true)
    }

    override fun initDataTracker() {
        super.initDataTracker()
    }

    override fun initialize(
        world: ServerWorldAccess,
        difficulty: LocalDifficulty,
        spawnReason: SpawnReason,
        entityData: EntityData?,
        entityNbt: NbtCompound?,
    ): EntityData? {
        if (spawnReason == SpawnReason.COMMAND ||
            spawnReason == SpawnReason.SPAWN_EGG ||
            SpawnReason.isAnySpawner(spawnReason) ||
            spawnReason == SpawnReason.DISPENSER
        ) {
        }
        SettlementManager.setProfession(this)
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    override fun initGoals() {
        targetSelector.add(0, PercieveGoal(this))
        goalSelector.add(0, ReactGoal(this))
        goalSelector.add(0, PlanGoal(this))
        goalSelector.add(0, ActGoal(this))
    }

    override fun getDimensions(pose: EntityPose): EntityDimensions =
        if (pose == EntityPose.SLEEPING) {
            SLEEPING_DIMENSIONS
        } else if (pose == EntityPose.SITTING) {
            SITTING_DIMENSIONS
        } else {
            STANDING_DIMENSIONS
        }

    override fun createNavigation(world: World): EntityNavigation = VillagerNavigation(this, world)

    fun hasVillage(): Boolean = this.data.key != -1 && this.data.sid != -1

    override fun wakeUp() {
        super.wakeUp()
    }

    override fun sleep(pos: BlockPos) {
        super.sleep(pos)
    }

    private var fighting: Boolean = false

    fun isFighting(): Boolean = fighting

    fun setFighting(fighting: Boolean) {
        this.fighting = fighting
    }

    fun isSitting(): Boolean = this.pose == EntityPose.SITTING

    fun sit(pos: BlockPos?) {
        pos?.let { p ->
            val target = p.toCenterPos()
            this.setPosition(target.getX(), target.getY() - 0.3, target.getZ())
            this.setPose(EntityPose.SITTING)
        } ?: run {
            val target = blockPos.toCenterPos()
            this.setPosition(target.getX(), target.getY() - 0.3, target.getZ())
            this.setPose(EntityPose.SITTING)
        }
    }

    fun getUp() {
        this.setPose(EntityPose.STANDING)
    }

    fun isEating(): Boolean {
        val stack = this.getActiveItem()
        return stack.getUseAction() == UseAction.EAT
    }

    override fun eatFood(
        world: World,
        stack: ItemStack,
    ): ItemStack {
        world.playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            SoundEvents.ENTITY_PLAYER_BURP,
            SoundCategory.PLAYERS,
            0.5F,
            world.random.nextFloat() * 0.1F + 0.9F,
        )
        if (stack.isFood()) {
            stack.decrement(1)
        }
        super.eatFood(world, stack)
        return stack
    }

    override fun getSwimHeight(): Double = 0.6

    override fun getUnscaledRidingOffset(vehicle: Entity): Float = -0.6f

    override fun getHandItems(): Iterable<ItemStack> = this.inventory.getHeldField()

    override fun getArmorItems(): Iterable<ItemStack> = this.inventory.getArmorField()

    fun getArmorItem(id: Int): ItemStack = this.inventory.getArmorField()[id]

    fun getHandItem(id: Int): ItemStack = this.inventory.getHeldField()[id]

    fun setArmorItem(
        id: Int,
        itemStack: ItemStack,
    ): ItemStack = this.inventory.setArmorField(id, itemStack)

    fun setHandItem(
        id: Int,
        itemStack: ItemStack,
    ): ItemStack = this.inventory.setHeldField(id, itemStack)

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack =
        when (slot.type) {
            EquipmentSlot.Type.HAND -> this.getHandItem(slot.entitySlotId)
            EquipmentSlot.Type.ARMOR -> this.getArmorItem(slot.entitySlotId)
            else -> ItemStack.EMPTY
        }

    override fun equipStack(
        slot: EquipmentSlot,
        stack: ItemStack,
    ) {
        this.processEquippedStack(stack)
        when (slot.type) {
            EquipmentSlot.Type.HAND -> {
                this.onEquipStack(slot, this.setHandItem(slot.entitySlotId, stack), stack)
            }
            EquipmentSlot.Type.ARMOR -> {
                this.onEquipStack(slot, this.setArmorItem(slot.entitySlotId, stack), stack)
            }
            else -> ItemStack.EMPTY
        }
    }

    fun getProfession(): Profession? =
        if (::profession.isInitialized) {
            this.profession
        } else {
            null
        }

    fun setProfession(type: ProfessionType) {
        LOGGER.info("SETTING PROFESSION: {}", type.name)
        this.profession = Profession.get(this, type)
    }

    fun getStructureOfInterest() = this.profession.structureInterest

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
    ): ScreenHandler? = TradingScreenHandler(syncId, playerInventory)

    override fun interactMob(
        player: PlayerEntity,
        hand: Hand,
    ): ActionResult {
        val itemStack: ItemStack = player.getStackInHand(hand)
        if (!itemStack.isOf(Items.VILLAGER_SPAWN_EGG) && isAlive && !isSleeping) {
            if (!world.isClient) {
                if (this.profession.type == ProfessionType.MERCHANT) {
                    val optionalInt = player.openHandledScreen(this)
                    // TODO: If recieved optionalInt int do calculations for trading
                }
            }
            return ActionResult.success(this.getWorld().isClient)
        }
        return super.interactMob(player, hand)
    }

    override fun tickMovement() {
        this.tickHandSwing()
        super.tickMovement()
    }

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean = false

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
    }

    override fun canPickUpLoot(): Boolean = true

    override fun canEquip(stack: ItemStack): Boolean = true

    override fun canGather(stack: ItemStack): Boolean =
        this.profession.desiredItems.any { p -> p(stack.item) } && this.inventory.canInsert(stack)

    override fun loot(item: ItemEntity) {
        this.inventory.pickUpItem(item)
    }

    override fun damageArmor(
        source: DamageSource,
        amount: Float,
    ) {
        this.inventory.damageArmor(source, amount, intArrayOf(0, 1, 2, 3))
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        this.inventory.dropAll()
        if (hasVillage()) {
            // SettlementManager.leaveSettlement(this)
        }
        super.onDeath(damageSource)
    }

    fun canAttack(): Boolean {
        this.getProfession()?.let { profession ->
            return profession.type == ProfessionType.RECRUIT
        }
        return false
    }

    fun canSleep(): Boolean = this.world.getTimeOfDay() % 24000.0f / 1000 > 12

    // TODO: use better nbt string name convetion
    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains(INVENTORY_KEY, NbtElement.LIST_TYPE.toInt())) {
            this.inventory.readNbt(nbt.getList(INVENTORY_KEY, NbtElement.COMPOUND_TYPE.toInt()))
        }
        // add check for first spawn
        this.setProfession(ProfessionType.values()[nbt.getInt("VillagerProfession")])
        if (nbt.contains("SettlementID", NbtElement.LIST_TYPE.toInt())) {
            this.data.sid = nbt.getInt("SettlementID")
            this.data.key = nbt.getInt("SettlementKey")
            this.data.dim = nbt.getString("SettlementDim")
        }
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.put(INVENTORY_KEY, this.inventory.writeNbt())
        nbt.putInt("VillagerProfession", this.profession.type.ordinal)
        nbt.putInt("SettlementID", this.data.sid)
        nbt.putInt("SettlementKey", this.data.key)
        nbt.putInt("SettlementDim", this.data.key)
    }

    companion object {
        const val INVENTORY_KEY = "Inventory"
        const val VILLAGER_PROFESSION = "VillagerProfession"

        val SITTING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.35f)
        val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.95f)

        fun createCustomVillagerAttributes(): DefaultAttributeContainer.Builder =
            PathAwareEntity
                .createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.5)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
    }
}
