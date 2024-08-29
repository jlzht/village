package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
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
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
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

// TODO:
// - rename CustomVillagerEntity to AbstractVillagerEntity
// - make it suitable for preferred hand selection
// - add eating action and methods
class CustomVillagerEntity(entityType: EntityType<out CustomVillagerEntity>, world: World?) :
    PathAwareEntity(entityType, world),
    ExtendedScreenHandlerFactory {

    private fun calculatePriority(): (Errand) -> Double = { (cid, p) ->
        Action.get(cid).scan(this, p).toDouble() //+ (1 - (this.blockPos.getManhattanDistance(p) / 2048.0))
    }

    private val errandsManager = ErrandManager(this, { null }, { null }, calculatePriority())

    fun getErrandsManager(): ErrandManager {
        return errandsManager
    }

    val inventory: VillagerInventory = VillagerInventory(this)
    private lateinit var profession: Profession

    var key: Int = -1
    var villageKey: Int = -1

    init {
        this.getNavigation().setCanSwim(false)
        (this.getNavigation() as MobNavigation).setCanPathThroughDoors(true)
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(CHARGING, false)
    }

    override fun initialize(world: ServerWorldAccess, difficulty: LocalDifficulty, spawnReason: SpawnReason, entityData: EntityData?, entityNbt: NbtCompound?): EntityData? {
        if (spawnReason == SpawnReason.COMMAND || spawnReason == SpawnReason.SPAWN_EGG || SpawnReason.isAnySpawner(spawnReason) || spawnReason == SpawnReason.DISPENSER) {
        }
        SettlementManager.setProfession(this)
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    override fun initGoals() {
        goalSelector.add(0, PercieveGoal(this))
        goalSelector.add(0, ReactGoal(this))
        goalSelector.add(0, ActGoal(this))
        goalSelector.add(1, PlanGoal(this))
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

    override fun createNavigation(world: World): EntityNavigation {
        return VillagerNavigation(this, world)
    }

    fun hasVillage(): Boolean {
        return this.key != 0 && this.villageKey != 0
    }

    override fun wakeUp() {
        super.wakeUp()
    }
    override fun sleep(pos: BlockPos) {
        super.sleep(pos)
    }

    fun isSitting(): Boolean {
        return this.pose == EntityPose.SITTING
    }

    fun sit(pos: BlockPos) {
        val target = pos.toCenterPos()
        this.setPosition(target.getX(), target.getY(), target.getZ())
        this.setPose(EntityPose.SITTING)
    }

    fun isEating(): Boolean {
        val stack = this.getActiveItem()
        return stack.getUseAction() == UseAction.EAT
    }

    override fun eatFood(world: World, stack: ItemStack): ItemStack {
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F)
        if (stack.isFood()) {
            stack.decrement(1)
        }
        super.eatFood(world, stack)
        return stack
    }

    override fun getSwimHeight(): Double {
        return 0.6
    }

    override fun getUnscaledRidingOffset(vehicle: Entity): Float {
        return -0.6f
    }

    override fun getHandItems(): Iterable<ItemStack> {
        return this.inventory.getHeldField()
    }

    override fun getArmorItems(): Iterable<ItemStack> {
        return this.inventory.getArmorField()
    }

    fun getArmorItem(id: Int): ItemStack {
        return this.inventory.getArmorField()[id]
    }

    fun getHandItem(id: Int): ItemStack {
        return this.inventory.getHeldField()[id]
    }

    fun setArmorItem(id: Int, itemStack: ItemStack): ItemStack {
        return this.inventory.setArmorField(id, itemStack)
    }
    fun setHandItem(id: Int, itemStack: ItemStack): ItemStack {
        return this.inventory.setHeldField(id, itemStack)
    }

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
        return when (slot.type) {
            EquipmentSlot.Type.HAND -> this.getHandItem(slot.entitySlotId)
            EquipmentSlot.Type.ARMOR -> this.getArmorItem(slot.entitySlotId)
            else -> ItemStack.EMPTY
        }
    }

    override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {
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

    fun getProfession(): Profession? {
        return if (::profession.isInitialized) { this.profession } else { null }
    }

    fun setProfession(type: ProfessionType) {
        LOGGER.info("SETTING PROFESSION: {}", type.name)
        this.profession = Profession.get(this, type)
    }

    fun getStructureOfInterest() = this.profession.structureInterest

    // get rid of this method
    fun isHoldingToolR(): Boolean {
        return !this.getStackInHand(Hand.MAIN_HAND).isEmpty()
    }

    fun isHoldingToolL(): Boolean {
        return !this.getStackInHand(Hand.OFF_HAND).isEmpty()
    }


    // get rid of this method
    fun isHoldingSword(): Boolean {
        return this.getStackInHand(Hand.MAIN_HAND).getItem() is SwordItem
    }

    // get rid of this method
    fun setCharging(charging: Boolean) {
        dataTracker.set(CHARGING, charging)
    }

    // get rid of this method
    fun isCharging(): Boolean {
        return dataTracker.get(CHARGING)
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(this.getBlockPos())
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler? {
        return TradingScreenHandler(syncId, playerInventory)
    }

    override fun interactMob(player: PlayerEntity, hand: Hand): ActionResult {
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

    override fun tick() {
        super.tick()
    }

    override fun tickMovement() {
        this.tickHandSwing()
        super.tickMovement()
    }

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
    }

    override fun canPickUpLoot(): Boolean {
        return true
    }

    override fun canEquip(stack: ItemStack): Boolean {
        return true
    }

    override fun canGather(stack: ItemStack): Boolean {
        return this.profession.desiredItems.any { p -> p(stack.item) } && this.inventory.canInsert(stack)
    }

    override fun loot(item: ItemEntity) {
        this.inventory.pickUpItem(item)
    }
    override fun damageArmor(source: DamageSource, amount: Float) {
        this.inventory.damageArmor(source, amount, intArrayOf(0, 1, 2, 3))
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        this.inventory.dropAll()
        if (hasVillage()) {
            SettlementManager.leaveSettlement(this)
        }
        super.onDeath(damageSource)
    }

    fun canAttack(): Boolean {
        this.getProfession()?.let { profession ->
            return profession.type == ProfessionType.RECRUIT
        }
        return false
    }

    fun canSleep(): Boolean {
        return this.world.getTimeOfDay() % 24000.0f / 1000 > 12
    }

    // TODO: use better nbt string name convetion
    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains(INVENTORY_KEY, NbtElement.LIST_TYPE.toInt())) {
            this.inventory.readNbt(nbt.getList(INVENTORY_KEY, NbtElement.COMPOUND_TYPE.toInt()))
        }
        LOGGER.info("READING THIS INSTEAD")
        this.setProfession(ProfessionType.values()[nbt.getInt("VillagerProfession")])
        this.villageKey = nbt.getInt("VillageKey")
        this.key = nbt.getInt("Key")
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.put(INVENTORY_KEY, this.inventory.writeNbt())
        nbt.putInt("VillagerProfession", this.profession.type.ordinal)
        nbt.putInt("VillageKey", this.villageKey)
        nbt.putInt("Key", this.key)
    }

    companion object {
        const val INVENTORY_KEY = "Inventory"
        const val VILLAGER_PROFESSION = "VillagerProfession"

        val CHARGING: TrackedData<Boolean> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.BOOLEAN,
            )

        val SITTING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.35f)
        val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.95f)

        fun createCustomVillagerAttributes(): DefaultAttributeContainer.Builder {
            return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.5)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0)
        }
    }
}
