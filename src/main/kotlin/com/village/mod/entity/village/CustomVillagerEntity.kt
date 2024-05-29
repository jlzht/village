package com.village.mod.entity.village

import com.village.mod.LOGGER
import com.village.mod.entity.ai.EntityWithProfession
import com.village.mod.entity.ai.goal.ActionGoal
import com.village.mod.entity.ai.goal.AttackGoal
import com.village.mod.entity.ai.goal.GotoGoal
import com.village.mod.entity.ai.goal.SenseGoal
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.structure.Structure
import com.village.mod.village.villager.State
import com.village.mod.world.event.VillagerKilledCallback
import com.village.mod.world.event.VillagerSpawnedCallback
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.enchantment.EnchantmentHelper
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
import net.minecraft.entity.ai.pathing.MobNavigation
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
import net.minecraft.item.ArmorItem
import net.minecraft.item.BlockItem
import net.minecraft.item.BowItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.MiningToolItem
import net.minecraft.item.RangedWeaponItem
import net.minecraft.item.SwordItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LocalDifficulty
import net.minecraft.world.ServerWorldAccess
import net.minecraft.world.World
import kotlin.random.Random

// placeholder data class
data class MutablePair(var key: Int, var structure: Structure?)

interface StatefulEntity

interface InventoryUser : InventoryOwner {
    fun takeItem(inventoryOwner: InventoryOwner, predicate: (Item) -> Boolean): ItemStack {
        val inventory = inventoryOwner.getInventory()
        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)
            if (predicate(stack.item)) {
                inventory.removeStack(i)
                return stack
            }
        }
        return ItemStack.EMPTY
    }

    companion object {
        val hoePredicate: (Item) -> Boolean = { item -> item is HoeItem }
        val bowPredicate: (Item) -> Boolean = { item -> item is BowItem }
        val swordPredicate: (Item) -> Boolean = { item -> item is SwordItem }
        val crossbowPredicate: (Item) -> Boolean = { item -> item is CrossbowItem }
        val rangedPredicate: (Item) -> Boolean = { item -> item is RangedWeaponItem }
    }
}

public class CustomVillagerEntity(entityType: EntityType<out CustomVillagerEntity>, world: World?) :
    PathAwareEntity(entityType, world),
    ExtendedScreenHandlerFactory,
    EntityWithProfession,
    InventoryUser {

    companion object {
        val VILLAGER_STAT: TrackedData<Int> =
            DataTracker.registerData(
                CustomVillagerEntity::class.java,
                TrackedDataHandlerRegistry.INTEGER,
            )
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
        dataTracker.startTracking(VILLAGER_STAT, 0)
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

    public var key: Int = 0
    public var mark: Int = 0
    public var villageKey: Int = 0
    public var experience: Int = 0
    public var emeraldShard: Int = 0

    // make village class hold village refs?
    public var homeStructure = MutablePair(0, null)
    public var workStructure = MutablePair(0, null)
    public fun ishomeok(): Boolean {
        return this.homeStructure.structure != null
    }
    public fun isworkok(): Boolean {
        return this.workStructure.structure != null
    }
    public val errand = ActionManager()
    public val state = StateManager(this)
    public val traitA = 1.0f
    public val traitB = 0.5f
    public fun shouldSleep(time: Float): Boolean {
        val value = Math.cos(time / (4.0 + traitA) + traitB)
        return Math.floor(value) == -1.0
    }
    public override fun wakeUp() {
        this.state.set(State.IDLE)
        super.wakeUp()
    }
    public override fun sleep(pos: BlockPos) {
        this.state.set(State.SLEEP)
        super.sleep(pos)
    }
    public override fun isSleeping(): Boolean {
        return this.state.isAt(State.SLEEP)
    }

    public fun sit(pos: BlockPos) {
        val destPos = pos.toCenterPos()
        this.setPosition(destPos.getX(), destPos.getY(), destPos.getZ())
        this.setPose(EntityPose.SITTING)
    }

    public override fun getSwimHeight(): Double {
        return 0.6
    }

    private val heldItems = DefaultedList.ofSize(2, ItemStack.EMPTY)
    private val armorItems = DefaultedList.ofSize(4, ItemStack.EMPTY)

    override fun getHandItems(): Iterable<ItemStack> {
        return this.heldItems
    }

    override fun getArmorItems(): Iterable<ItemStack> {
        return this.armorItems
    }

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
        return when (slot.type) {
            EquipmentSlot.Type.HAND -> this.heldItems[slot.entitySlotId]
            EquipmentSlot.Type.ARMOR -> this.armorItems[slot.entitySlotId]
            else -> ItemStack.EMPTY
        }
    }

    override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {
        this.processEquippedStack(stack)
        when (slot.type) {
            EquipmentSlot.Type.HAND -> {
                if (this.inventory.canInsert(this.heldItems.get(slot.entitySlotId))) {
                    this.inventory.addStack(this.heldItems.get(slot.entitySlotId))
                } else {
                    this.dropStack(this.heldItems.get(slot.entitySlotId))
                }
                this.onEquipStack(slot, this.heldItems.set(slot.entitySlotId, stack), stack)
            }
            EquipmentSlot.Type.ARMOR -> {
                if (this.inventory.canInsert(this.armorItems.get(slot.entitySlotId))) {
                    this.inventory.addStack(this.armorItems.get(slot.entitySlotId))
                } else {
                    this.dropStack(this.armorItems.get(slot.entitySlotId))
                }
                this.onEquipStack(slot, this.armorItems.set(slot.entitySlotId, stack), stack)
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
        this.profession.addProfessionTasks(this)
        this.setVillagerData(profession.type.ordinal)
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
        setProfession(ProfessionType.values()[Random.nextInt(5)])
        VillagerSpawnedCallback.EVENT.invoker().interact(this)
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt)
    }

    public fun appendGoal(priority: Int, goal: Goal) {
        this.goalSelector.add(priority, goal)
    }
    public fun appendTargetGoal(priority: Int, goal: Goal) {
        this.targetSelector.add(priority, goal)
    }

    override fun initGoals() {
        goalSelector.add(0, GotoGoal(this))
        goalSelector.add(1, SenseGoal(this))
        goalSelector.add(1, AttackGoal(this, 1.0))
        goalSelector.add(1, ActionGoal(this))
    }

    fun setCharging(charging: Boolean) {
        dataTracker.set(CHARGING, charging)
    }

    fun isCharging(): Boolean {
        return dataTracker.get(CHARGING)
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
    public fun setActing(acting: Boolean) {
        this.acting = acting
    }
    public fun isActing(): Boolean {
        return this.acting
    }

    override fun tick() {
        if (!this.errand.isEmpty()) {
        }
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

    private val inventory: SimpleInventory = SimpleInventory(9)

    public override fun getInventory(): SimpleInventory {
        return this.inventory
    }

    fun dropInventoryItems(world: World, pos: BlockPos) {
        ItemScatterer.spawn(world, pos, this.getInventory())
    }

    public override fun canPickUpLoot(): Boolean {
        return true
    }

    public override fun canEquip(stack: ItemStack): Boolean {
        return true
    }

    override fun canGather(stack: ItemStack): Boolean {
        return this.profession.desiredItems(stack.item) && this.inventory.canInsert(stack)
    }

    override fun tryEquip(stack: ItemStack): ItemStack {
        var equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack)
        var itemStack = getEquippedStack(equipmentSlot)
        var prefersNew = prefersNewEquipment(stack, itemStack)
        if (prefersNew && canPickupItem(stack)) {
            // ItemScatterer.spawn(world, pos.x, pos.y + 1.0, pos.z, equipment)
            if (this.inventory.canInsert(itemStack)) {
                this.inventory.addStack(itemStack)
            } else if (!itemStack.isEmpty) {
                this.dropStack(itemStack)
            }
            if (equipmentSlot.isArmorSlot && stack.count > 1) {
                val itemStack2 = stack.copy().apply { count = 1 }
                equipStack(equipmentSlot, itemStack2)
                return itemStack2
            }
            equipStack(equipmentSlot, stack)
            return stack
        }
        return ItemStack.EMPTY
    }

    protected override fun prefersNewEquipment(newStack: ItemStack, oldStack: ItemStack): Boolean {
        if (oldStack.isEmpty) {
            return true
        }
        val newItem = newStack.item
        val oldItem = oldStack.item

        return when (newItem) {
            is SwordItem -> when (oldItem) {
                is SwordItem -> {
                    if (newItem.attackDamage != oldItem.attackDamage) {
                        newItem.attackDamage > oldItem.attackDamage
                    } else {
                        prefersNewDamageableItem(newStack, oldStack)
                    }
                }
                else -> true
            }
            is BowItem, is CrossbowItem -> prefersNewDamageableItem(newStack, oldStack)
            is ArmorItem -> {
                if (EnchantmentHelper.hasBindingCurse(oldStack)) {
                    false
                } else {
                    val newArmorItem = newItem
                    val oldArmorItem = oldItem as ArmorItem
                    if (newArmorItem.protection != oldArmorItem.protection) {
                        newArmorItem.protection > oldArmorItem.protection
                    } else if (newArmorItem.toughness != oldArmorItem.toughness) {
                        newArmorItem.toughness > oldArmorItem.toughness
                    } else {
                        prefersNewDamageableItem(newStack, oldStack)
                    }
                }
            }
            is MiningToolItem -> {
                when (oldItem) {
                    is BlockItem -> true
                    is MiningToolItem -> {
                        if (newItem.attackDamage != oldItem.attackDamage) {
                            newItem.attackDamage > oldItem.attackDamage
                        } else {
                            prefersNewDamageableItem(newStack, oldStack)
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    override fun prefersNewDamageableItem(newStack: ItemStack, oldStack: ItemStack): Boolean {
        return if (newStack.damage < oldStack.damage || newStack.hasNbt() && !oldStack.hasNbt()) {
            true
        } else if (newStack.hasNbt() && oldStack.hasNbt()) {
            newStack.nbt!!.keys.any { it != "Damage" } && oldStack.nbt!!.keys.none { it != "Damage" }
        } else {
            false
        }
    }

    protected override fun loot(item: ItemEntity) {
        InventoryOwner.pickUpItem(this, this, item)
    }

    override fun onDeath(damageSource: DamageSource) {
        LOGGER.info("Villager {} died, message: {}", this as Any, damageSource.getDeathMessage(this).string)
        // var entity: Entity? = damageSource.getAttacker()
        dropInventoryItems(world, this.getBlockPos().up())
        if (this.key != -1) {
            VillagerKilledCallback.EVENT.invoker().interact(this)
        }
        super.onDeath(damageSource)
    }
    public fun canAttack(): Boolean {
        return this.getProfession()?.type == ProfessionType.GUARD
    }

    fun encodeVillagerData(mark: Int, experience: Int, shards: Int, profession: Int, states: Int): Int {
        return (mark) or (experience shl 2) or ((shards shl 12)) or ((profession shl 22)) or ((states shl 28))
    }

    fun decodeVillagerData(villagerStat: Int): IntArray {
        return intArrayOf(mark and 3, villagerStat shr 2 and 1023, villagerStat shr 12 and 1023, villagerStat shr 22 and 31, villagerStat ushr 28 and 15)
    }

    fun setVillagerData(villagerData: Int) {
        this.dataTracker.set(VILLAGER_STAT, villagerData)
    }

    fun getVillagerData(): Int {
        return this.dataTracker.get(VILLAGER_STAT)
    }

    // TODO: use better nbt string name convetion
    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        this.readInventory(nbt)
        if (nbt.contains("VillagerDataZ")) {
            val arr = decodeVillagerData(nbt.getInt("VillagerDataZ"))
            LOGGER.info("READING -> {}, EXP:{}, EMERALD:{}, PROFESION:{}, STATE:{}", arr[0], arr[1], arr[2], State.values()[arr[3]], arr[4])
            this.mark = arr[0]
            this.experience = arr[1]
            this.emeraldShard = arr[2]
            setProfession(ProfessionType.values()[arr[3]])
            this.state.set(State.values()[arr[4]])
        }
        this.key = nbt.getInt("Key")
        this.villageKey = nbt.getInt("VillageKey")
        LOGGER.info("HOME: {}, WORK: {}", this.homeStructure.key, this.workStructure.key)
        this.workStructure = MutablePair(nbt.getInt("WorkStructureKey"), null)
        this.homeStructure = MutablePair(nbt.getInt("BuildingStructureKey"), null)
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        LOGGER.info("WRITING -> ID: {}, EXP: {}, EMERALD: {}, PROFESSION: {}, STATE: {}| HOME: {}, WORK:{}", this.key, this.experience, this.emeraldShard, this.getProfession()?.type, this.state.get(), this.homeStructure.key, this.workStructure.key)
        this.writeInventory(nbt)
        nbt.putInt(
            "VillagerDataZ",
            encodeVillagerData(
                this.mark,
                this.experience,
                this.emeraldShard,
                this.profession.type.ordinal,
                this.state.get().ordinal,
            ),
        )
        nbt.putInt("Key", this.key)
        nbt.putInt("VillageKey", this.villageKey)
        nbt.putInt("WorkStructureKey", this.workStructure.key)
        nbt.putInt("BuildingStructureKey", this.homeStructure.key)
    }
}
