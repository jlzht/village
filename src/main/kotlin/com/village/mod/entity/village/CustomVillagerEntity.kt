package com.village.mod.entity.village

import com.village.mod.BlockAction
import com.village.mod.LOGGER
import com.village.mod.entity.ai.goal.GotoGoal
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.Farmer
import com.village.mod.village.profession.Fisherman
import com.village.mod.village.profession.Guard
import com.village.mod.village.profession.Merchant
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.profession.Unemployed
import com.village.mod.village.villager.State
import com.village.mod.world.Home
import com.village.mod.world.Structure
import com.village.mod.world.event.VillagerKilledCallback
import com.village.mod.world.event.VillagerSpawnedCallback
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityData
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.InventoryOwner
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.RangedWeaponItem
import net.minecraft.item.SwordItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LocalDifficulty
import net.minecraft.world.ServerWorldAccess
import net.minecraft.world.World
import kotlin.random.Random

public class CustomVillagerEntity(entityType: EntityType<out CustomVillagerEntity>, world: World?) :
    PathAwareEntity(entityType, world),
    ExtendedScreenHandlerFactory,
    InventoryOwner {

    companion object {
        val CHARGING: TrackedData<Boolean> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.BOOLEAN,
            )
        val STATE: TrackedData<Int> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.INTEGER,
            )
        val VILLAGER_STAT: TrackedData<Int> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.INTEGER,
            )

        fun createCustomVillagerAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0)
        }
    }

    protected val SITTING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.35f)
    protected val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.fixed(0.6f, 1.95f)

    private val ATTACK_RANGE: Double = 3.0
    private var profession: Profession = Unemployed()
    public var mark: Int = 0
    public var key: Int = 0
    public var villageKey: Int = 0
    public var homeStructure: Pair<Int, Structure?> = Pair(0, null)
    public var workStructure: Pair<Int, Structure?> = Pair(0, null)
    private val inventory: SimpleInventory = SimpleInventory(18)
    private var targetBlock: HashSet<BlockAction> = hashSetOf()
    public var emeraldShard: Int = 0
    public var experience: Int = 0
    private var lockState: Boolean = false
    init {
        this.getNavigation().setCanSwim(false)
    }

    public fun assignStructure(key: Int, structure: Structure) {
        if (structure is Home) {
            this.homeStructure = Pair(key, structure)
        } else {
            this.workStructure = Pair(key, structure)
        }
    }

    override fun initialize(world: ServerWorldAccess, difficulty: LocalDifficulty, spawnReason: SpawnReason, entityData: EntityData?, entityNbt: NbtCompound?): EntityData? {
        if (spawnReason == SpawnReason.COMMAND || spawnReason == SpawnReason.SPAWN_EGG || SpawnReason.isAnySpawner(spawnReason) || spawnReason == SpawnReason.DISPENSER) {
        }
        setProfession(Random.nextInt(5))
        VillagerSpawnedCallback.EVENT.invoker().interact(this)
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    public fun addTargetBlock(action: BlockAction) {
        LOGGER.info("VILLAGER({}): ADDING ACTION FOR BLOCK: {} AT: {}", this.key, action.block, action.pos)
        this.targetBlock.add(action)
    }

    public fun delTargetBlock(action: BlockAction) {
        this.targetBlock.remove(action)
    }
    public fun getTargetBlock(): BlockAction {
        return this.targetBlock.first()
    }
    public fun isTargetBlockEmpty(): Boolean {
        return this.targetBlock.isEmpty()
    }

    public override fun canGather(stack: ItemStack): Boolean {
        return this.inventory.canInsert(stack)
    }
    override fun remove(reason: RemovalReason) {
        super.remove(reason)
    }

    public fun setVillagerData(villagerData: Int) {
        this.dataTracker.set(VILLAGER_STAT, villagerData)
    }

    public fun getVillagerData(): Int {
        return this.dataTracker.get(VILLAGER_STAT)
    }

    public fun getState(): State {
        return State.values()[dataTracker.get(STATE)]
    }

    public fun setState(state: State) {
        if (this.getState() != state) {
            dataTracker.set(STATE, state.ordinal)
        }
    }

    public fun isState(state: State): Boolean {
        return state == this.getState()
    }

    public fun lockState() {
        this.lockState = true
    }
    public fun unlockState() {
        this.lockState = false
    }
    public fun getLockState(): Boolean {
        return this.lockState
    }

    public fun appendGoal(priority: Int, goal: Goal) {
        this.goalSelector.add(priority, goal)
    }
    public fun appendTargetGoal(priority: Int, goal: Goal) {
        this.targetSelector.add(priority, goal)
    }

    public fun isHoldingTool(): Boolean {
        return !this.getStackInHand(Hand.MAIN_HAND).isEmpty()
    }

    override fun tickMovement() {
        this.tickHandSwing()
        super.tickMovement()
    }

    fun setCharging(charging: Boolean) {
        dataTracker.set(CHARGING, charging)
    }

    fun isCharging(): Boolean {
        return dataTracker.get(CHARGING)
    }

    fun getProfession(): Profession {
        return this.profession
    }

    fun setProfession(n: Int) {
        val professionNumber = ProfessionType.values()[n]
        this.profession = when (professionNumber) {
            ProfessionType.NONE -> Unemployed()
            ProfessionType.FARMER -> Farmer()
            ProfessionType.FISHERMAN -> Fisherman()
            ProfessionType.MERCHANT -> Merchant()
            ProfessionType.GUARD -> Guard()
        }
        profession.addProfessionTasks(this)
        this.setVillagerData(n)
        LOGGER.info("{} - {}", professionNumber, this.getVillagerData())
        val stackInHand = this.getStackInHand(Hand.MAIN_HAND)
        if (stackInHand.isEmpty && professionNumber == ProfessionType.FISHERMAN) {
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack(Items.FISHING_ROD))
        }
        if (stackInHand.isEmpty && professionNumber == ProfessionType.GUARD) {
            this.getInventory().setStack(0, ItemStack(Items.WOODEN_SWORD))
            when (Random.nextInt(2)) {
                0 -> this.getInventory().setStack(1, ItemStack(Items.BOW))
                1 -> this.getInventory().setStack(1, ItemStack(Items.CROSSBOW))
            }
        }
        if (stackInHand.isEmpty && professionNumber == ProfessionType.FARMER) {
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack(Items.WOODEN_HOE))
        }
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
        goalSelector.add(2, GotoGoal(this))
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(CHARGING, false)
        dataTracker.startTracking(STATE, State.IDLE.ordinal)
        dataTracker.startTracking(VILLAGER_STAT, 0)
    }
    override fun getUnscaledRidingOffset(vehicle: Entity): Float {
        return -0.6f
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
                if (this.profession is Merchant) {
                    val optionalInt = player.openHandledScreen(this)
                    // TODO: If recieved optionalInt int do calculations for trading
                }
            }
            return ActionResult.success(this.getWorld().isClient)
        }
        return super.interactMob(player, hand)
    }

    public override fun tick() {
        super.tick()
    }

    fun dropInventoryItems(world: World, pos: BlockPos) {
        ItemScatterer.spawn(world, pos, this.getInventory())
    }

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        // var entity: Entity? = damageSource.getAttacker()
        dropInventoryItems(world, this.getBlockPos())
        VillagerKilledCallback.EVENT.invoker().interact(this)
        super.onDeath(damageSource)
    }

    public override fun getInventory(): SimpleInventory {
        return this.inventory
    }

    public override fun canPickUpLoot(): Boolean {
        return true
    }

    protected override fun loot(item: ItemEntity) {
        InventoryOwner.pickUpItem(this, this, item)
    }

    fun encodeVillagerData(mark: Int, experience: Int, shards: Int, profession: Int, states: Int): Int {
        return (mark) or (experience shl 2) or ((shards shl 12)) or ((profession shl 22)) or ((states shl 28))
    }

    fun decodeNbtData(villagerStat: Int): IntArray {
        return intArrayOf(mark and 3, villagerStat shr 2 and 1023, villagerStat shr 12 and 1023, villagerStat shr 22 and 31, villagerStat ushr 28 and 15)
    }

    val swordPredicate: (Item) -> Boolean = { item -> item is SwordItem }
    val rangedPredicate: (Item) -> Boolean = { item -> item is RangedWeaponItem }

    fun takeItem(predicate: (Item) -> Boolean): ItemStack {
        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)
            if (predicate(stack.item)) {
                inventory.removeStack(i)
                return stack
            }
        }
        return ItemStack.EMPTY
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains("VillagerDataZ")) {
            val arr = decodeNbtData(nbt.getInt("VillagerDataZ"))
            LOGGER.info("READING -> {}, EXP:{}, EMERALD:{}, PROFESION:{}, STATE:{}", arr[0], arr[1], arr[2], State.values()[arr[3]], arr[4])
            this.mark = arr[0]
            this.experience = arr[1]
            this.emeraldShard = arr[2]
            setProfession(arr[3])
            setState(State.values()[arr[4]])
        }
        this.key = nbt.getInt("Key")
        this.villageKey = nbt.getInt("VillageKey")
        LOGGER.info("HOME: {}, WORK: {}", this.homeStructure.first, this.workStructure.first)
        this.workStructure = Pair(nbt.getInt("WorkStructureKey"), null)
        this.homeStructure = Pair(nbt.getInt("HomeStructureKey"), null)
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        LOGGER.info("WRITING -> ID: {}, {} EXP: {}, EMERALD: {}, PROFESSION: {}, STATE: {}| HOME: {}, WORK:{}", this.key, this.mark, this.experience, this.emeraldShard, this.getProfession().type, this.getState(),this.homeStructure.first, this.workStructure.first)
        nbt.putInt(
            "VillagerDataZ",
            encodeVillagerData(
                this.mark,
                this.experience,
                this.emeraldShard,
                this.getProfession().type.ordinal,
                this.getState().ordinal,
            ),
        )
        nbt.putInt("Key", this.key)
        nbt.putInt("VillageKey", this.villageKey)
        nbt.putInt("WorkStructureKey", this.workStructure.first)
        nbt.putInt("HomeStructureKey", this.homeStructure.first)
    }
}
