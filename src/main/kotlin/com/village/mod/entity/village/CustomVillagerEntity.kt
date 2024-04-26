package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.entity.ai.goal.SitGoal
import com.village.mod.entity.ai.goal.GotoGoal
import com.village.mod.entity.ai.goal.SleepGoal
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.Farmer
import com.village.mod.village.profession.Fisherman
import com.village.mod.village.profession.Guard
import com.village.mod.village.profession.Merchant
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.profession.Unemployed
import com.village.mod.village.villager.State
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
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
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
import java.util.ArrayDeque
import java.util.Deque

public class CustomVillagerEntity(entityType: EntityType<out CustomVillagerEntity>, world: World?) :
    PathAwareEntity(
        entityType,
        world,
    ),
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

    private var profession: Profession = Unemployed()
    public var  mark: Int = 0
    public var identification: Int = 0
    public var villageIdentification: Int = 0
    private val inventory: SimpleInventory = SimpleInventory(18)
    private var lockState: Boolean = false
    public var zoneOfInterest: MutableList<BlockPos> = mutableListOf(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
    private var targetBlock: MutableList<BlockPos> = mutableListOf()
    public var emeraldShard: Int = 0
    public var experience: Int = 0

    override fun initialize(world: ServerWorldAccess, difficulty: LocalDifficulty, spawnReason: SpawnReason, entityData: EntityData?, entityNbt: NbtCompound?): EntityData? {
        if (spawnReason == SpawnReason.COMMAND || spawnReason == SpawnReason.SPAWN_EGG || SpawnReason.isAnySpawner(spawnReason) || spawnReason == SpawnReason.DISPENSER) {
        }
        setProfession(Random.nextInt(5))
        val chance = Random.nextInt(1) * 3
        VillagerRequestCallback.EVENT.invoker().interact(this, chance)

        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    // Targeting

    public fun pushTargetBlock(blockPos: BlockPos) {
        if (!targetBlock.contains(blockPos)) {
          LOGGER.info("adding new blocks!")
          this.targetBlock.add(blockPos)
        }
    }

    public fun peekTargetBlock(): BlockPos {
        return this.targetBlock.first()
    }

    public fun popTargetBlock(): BlockPos {
        return this.targetBlock.removeFirst()
    }
    public fun isTargetBlockEmpty(): Boolean {
      return this.targetBlock.isEmpty()
    }
    public fun sortTargetBlock() {
      this.targetBlock = targetBlock.sorted().distinct().toMutableList()
    }

    public override fun canGather(stack: ItemStack): Boolean {
        return this.inventory.canInsert(stack)
    }
    override  fun remove(reason: RemovalReason) {
      super.remove(reason)
      LOGGER.info("I GOT REMOVED!")
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

    public fun tryAppendToZOI(blockPos: BlockPos) {
        if (zoneOfInterest[0].equals(zoneOfInterest[1])) {
            zoneOfInterest[1] = blockPos
            return
        }
        if (zoneOfInterest[0].x == 0 && zoneOfInterest[0].z == 0) {
            zoneOfInterest[0] = zoneOfInterest[1]
        }
        if (blockPos.x > zoneOfInterest[1].x) {
            zoneOfInterest[1] = zoneOfInterest[1].add(blockPos.x - zoneOfInterest[1].x, 0, 0)
        }
        if (blockPos.z < zoneOfInterest[1].z) {
            zoneOfInterest[1] = zoneOfInterest[1].add(0, 0, blockPos.z - zoneOfInterest[1].z)
        }
        if (blockPos.x < zoneOfInterest[0].x) {
            zoneOfInterest[0] = zoneOfInterest[0].add(blockPos.x - zoneOfInterest[0].x, 0, 0)
        }
        if (blockPos.z > zoneOfInterest[0].z) {
            zoneOfInterest[0] = zoneOfInterest[0].add(0, 0, blockPos.z - zoneOfInterest[0].z)
        }
        LOGGER.info("I GOT IN - {} --- {} -", zoneOfInterest[0], zoneOfInterest[1])
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
        //if (stackInHand.isEmpty && professionNumber == ProfessionType.FISHERMAN) {
        //    this.equipStack(EquipmentSlot.MAINHAND, ItemStack(Items.FISHING_ROD))
        //}
        if (stackInHand.isEmpty && professionNumber == ProfessionType.GUARD) {
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack(Items.BOW))
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
        // goalSelector.add(0, SwimGoal(this))
        // goalSelector.add(3, SleepGoal(this))
        // ADD IDLE TASK TO MAKE ENTITY MORE VIVID
        //goalSelector.add(4, SitGoal(this)) // MUST NOT BE A TASK
        goalSelector.add(2, GotoGoal(this))
        // goalSelector.add(4, WanderAroundGoal(this, 1.0))
        // goalSelector.add(5, LookAtEntityGoal(this, CustomVillagerEntity::class.java, 6.0f))
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
                    //TODO: If recieved optionalInt int do calculations for trading
                }
            }
            return ActionResult.success(this.getWorld().isClient)
        }
        return super.interactMob(player, hand)
    }

    public override fun tick() {
        super.tick()
    }

    fun dropInventoryItems(
        world: World,
        pos: BlockPos,
    ) {
        ItemScatterer.spawn(world, pos, this.getInventory())
    }

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        // var entity: Entity? = damageSource.getAttacker()
        dropInventoryItems(world, this.getBlockPos())
        VillagerRequestCallback.EVENT.invoker().interact(this, 1)
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

    fun encodeNbtData(mark: Int, experience: Int, shards: Int, profession: Int, states: Int): Int {
        return (mark) or (experience shl 2) or ((shards shl 12)) or ((profession shl 22)) or ((states shl 28))
    }

    fun decodeNbtData(villagerStat: Int): IntArray {
        return intArrayOf(mark and 3,villagerStat shr 2 and 1023, villagerStat shr 12 and 1023, villagerStat shr 22 and 31, villagerStat ushr 28 and 15)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains("villagerStat")) {
            val arr = decodeNbtData(nbt.getInt("villagerStat"))
            LOGGER.info("READ NBT: {}, {}, {}, {}, {}", arr[0], arr[1], arr[2], arr[3], arr[4])
            this.mark = arr[0]
            this.experience = arr[1]
            this.emeraldShard = arr[2]
            setProfession(arr[3])
            setState(State.values()[arr[4]])
        }
        // this.setState(State.values()[nbt.getInt("statew")])
        this.identification = nbt.getInt("iden")
        this.villageIdentification = nbt.getInt("villageIden")
        //if (nbt.contains("posX")) {
        //  if (this.getTargetBlock() == null) {
        //    this.setTargetBlock(BlockPos(
        //        nbt.getInt("posX"),
        //        nbt.getInt("posY"),
        //        nbt.getInt("posZ")
        //    ))
        //}
        //}
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        LOGGER.info("ID {} WRITING TO NBT: {}, {}, {}, {}, {}", this.identification, this.mark, this.experience, this.emeraldShard, this.getProfession().type.ordinal, this.getState().ordinal)
        nbt.putInt(
            "villagerStat",
            encodeNbtData(
                this.mark,
                this.experience,
                this.emeraldShard,
                this.getProfession().type.ordinal,
                this.getState().ordinal
            ),
        )
        // nbt.putInt("statew", this.getState().ordinal)
        //val tpos = this.getTargetBlock()
        //if (tpos != null) {
        //    nbt.putInt("posX", tpos.x)
        //    nbt.putInt("posY", tpos.y)
        //    nbt.putInt("posZ", tpos.z)
        //}
        nbt.putInt("iden", this.identification)
        nbt.putInt("villageIden", this.villageIdentification)
    }
}
