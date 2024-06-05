package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.entity.ai.EntityWithProfession
import com.village.mod.entity.ai.goal.ActGoal
import com.village.mod.entity.ai.goal.PercieveGoal
import com.village.mod.entity.ai.goal.PlanGoal
import com.village.mod.entity.ai.goal.ReactGoal
import com.village.mod.entity.ai.pathing.VillagerNavigation
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.structure.Structure
import com.village.mod.village.villager.State
import com.village.mod.world.event.VillagerKilledCallback
import com.village.mod.world.event.VillagerSpawnedCallback
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
import net.minecraft.entity.mob.MobEntity
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
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LocalDifficulty
import net.minecraft.world.ServerWorldAccess
import net.minecraft.world.World
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

// class VillagerData

// TODO: make it suitable for preferred hand selection
class CustomVillagerEntity(entityType: EntityType<out CustomVillagerEntity>, world: World?) :
    MobEntity(entityType, world), // can use LivingEntity directly?  -> less overhead -> migrate to task system
    ExtendedScreenHandlerFactory,
    EntityWithProfession {

    companion object {
        const val INVENTORY_KEY = "Inventory"
        const val VILLAGER_PROFESSION = "VillagerProfession"
        const val VILLAGER_STATE = "VillagerState"

        // val VILLAGER_STAT: TrackedData<Int> =
        //    DataTracker.registerData(
        //        CustomVillagerEntity::class.java,
        //        TrackedDataHandlerRegistry.INTEGER,
        //    )
        val CHARGING: TrackedData<Boolean> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.BOOLEAN,
            )
        fun createCustomVillagerAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.5)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0)
        }
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(CHARGING, false)
        // dataTracker.startTracking(VILLAGER_STAT, 0)
    }

    protected val SITTING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.35f)
    protected val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.95f)

    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return if (pose == EntityPose.SLEEPING) {
            SLEEPING_DIMENSIONS
        } else if (pose == EntityPose.SITTING) {
            SITTING_DIMENSIONS
        } else {
            STANDING_DIMENSIONS
        }
    }

    // var Node: Int
    override fun createNavigation(world: World): EntityNavigation {
        return VillagerNavigation(this, world)
    }

    var key: Int = 0
    var villageKey: Int = 0

    fun hasVillage(): Boolean {
        return this.key != -1
    }

    val structures: ArrayList<Structure> = arrayListOf()

    fun hasHome(): Boolean {
        return !this.structures.isEmpty()
    }

    fun hasWork(): Boolean {
        return this.structures.map { it.type }.contains(this.profession.structureInterest)
    }

    val inventory: VillagerInventory = VillagerInventory(this)

    val state = StateManager(this)
    val errand = ActionManager()

    // FIELD0 FIELD1 FIELD2
    val traitA = 1.0f
    val traitB = 0.5f

    //fun shouldSleep(time: Float): Boolean {
    //    val value = Math.cos(time / (4.0 + traitA) + traitB)
    //    return Math.floor(value) == -1.0
    //}

    // this.getErrandAction = | getSleepingAction() // getWorkingActions() //getIdlingAction() |
    fun updateGroupAction(time: Float) {
        val a = Math.sin(Math.PI * (time + 2 * (1 - this.traitA)) / (2 * (6 - this.traitB)))
        val b = Math.sin(Math.PI * ((time - 2) - (1 - this.traitA)) / (2 * (3 + this.traitB)))
        val g = floor(1 - a) * floor(2 - b)
        LOGGER.info("Group: {}", g)
        when (g) {
            0.0 -> {
                if (this.hasHome() && !this.isSleeping()) {
                    // NodeGraphManagerCallback // Request owned node to SLEEP
                    // VillageGraphNodeManager -> {}
                    // this.structures.getErrands(world)
                } // SLEEP
            }
            1.0 -> {
                if (this.hasWork() && this.profession.canWork()) {
                }
            } // WORK
            2.0 -> {
                if (this.isSleeping()) this.wakeUp()
            } // IDLE
        }
    }

    override fun wakeUp() {
        super.wakeUp()
        this.state.set(State.IDLE)
    }
    override fun sleep(pos: BlockPos) {
        this.state.set(State.SLEEP)
        super.sleep(pos)
    }
    override fun isSleeping(): Boolean {
        return this.state.isAt(State.SLEEP)
    }

    fun sit(pos: BlockPos) {
        val destPos = pos.toCenterPos()
        this.setPosition(destPos.getX(), destPos.getY(), destPos.getZ())
        this.state.set(State.SIT)
        this.setPose(EntityPose.SITTING)
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

    init {
        this.getNavigation().setCanSwim(false)
        (this.getNavigation() as MobNavigation).setCanPathThroughDoors(true)
    }

    // TODO: MAKE THIS NOT NULLABLE
    private lateinit var profession: Profession
    override fun getProfession(): Profession? {
        return if (::profession.isInitialized) { this.profession } else { null }
    }
    override fun setProfession(profession: Profession) {
        this.profession = profession
    }

    fun isHoldingTool(): Boolean {
        return !this.getStackInHand(Hand.MAIN_HAND).isEmpty()
    }

    fun isHoldingSword(): Boolean {
        return this.getStackInHand(Hand.MAIN_HAND).getItem() is SwordItem
    }

    override fun initialize(world: ServerWorldAccess, difficulty: LocalDifficulty, spawnReason: SpawnReason, entityData: EntityData?, entityNbt: NbtCompound?): EntityData? {
        if (spawnReason == SpawnReason.COMMAND || spawnReason == SpawnReason.SPAWN_EGG || SpawnReason.isAnySpawner(spawnReason) || spawnReason == SpawnReason.DISPENSER) {
        }
        // setProfession(this, ProfessionType.values()[Random.nextInt(5)])
        setProfession(this, ProfessionType.GUARD)
        VillagerSpawnedCallback.EVENT.invoker().interact(this)
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    override fun initGoals() {
        goalSelector.add(0, PercieveGoal(this))
        goalSelector.add(1, ReactGoal(this, 1.0))
        goalSelector.add(0, ActGoal(this))
        goalSelector.add(1, PlanGoal(this))
    }

    fun setCharging(charging: Boolean) {
        dataTracker.set(CHARGING, charging)
    }

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

    private var acting: Boolean = false
    fun setActing(acting: Boolean) {
        this.acting = acting
    }
    fun isActing(): Boolean {
        return this.acting
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
        return this.profession.desiredItems(stack.item) && this.inventory.canInsert(stack)
    }

    protected override fun loot(item: ItemEntity) {
        this.inventory.pickUpItem(item)
    }
    override fun damageArmor(source: DamageSource, amount: Float) {
        this.inventory.damageArmor(source, amount, intArrayOf(0, 1, 2, 3))
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        this.inventory.dropAll()
        // var entity: Entity? = damageSource.getAttacker()
        if (!hasVillage()) {
            VillagerKilledCallback.EVENT.invoker().interact(this)
        }
        super.onDeath(damageSource)
    }

    fun canAttack(): Boolean {
        return this.getProfession()?.type == ProfessionType.GUARD
    }

    // TODO: use better nbt string name convetion
    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains(INVENTORY_KEY, NbtElement.LIST_TYPE.toInt())) {
            this.inventory.readNbt(nbt.getList(INVENTORY_KEY, NbtElement.COMPOUND_TYPE.toInt()))
        }
        this.setProfession(this, ProfessionType.values()[nbt.getInt("VillagerProfession")])
        this.state.set(State.values()[nbt.getInt("VillagerState")])
        this.villageKey = nbt.getInt("VillageKey")
        this.key = nbt.getInt("Key")
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.put(INVENTORY_KEY, this.inventory.writeNbt())
        nbt.putInt("VillagerProfession", this.profession.type.ordinal)
        nbt.putInt("VillagerState", this.state.get().ordinal)
        nbt.putInt("VillageKey", this.villageKey)
        nbt.putInt("Key", this.key)
    }
}
