package com.village.mod.entity.projectile

import com.village.mod.LOGGER
import com.village.mod.Village
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.Fisherman
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.MovementType
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.fluid.FluidState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

class SimpleFishingBobberEntity(type: EntityType<out SimpleFishingBobberEntity>, world: World, luckOfTheSeaLevel: Int, lureLevel: Int) : ProjectileEntity(type, world) {
    private val velocityRandom: Random = Random.create()
    private var state: BobberState = BobberState.FLYING
    private var luckOfTheSeaLevel: Int = 0
    private var lureLevel: Int = 0
    private var removalTimer: Int = 0
    private var hookCountdown: Int = 0
    private var fishingTicks: Int = 0

    init {
        this.luckOfTheSeaLevel = maxOf(0, luckOfTheSeaLevel)
        this.lureLevel = maxOf(0, lureLevel)
        this.ignoreCameraFrustum = true
    }

    constructor(thrower: CustomVillagerEntity, world: World, luckOfTheSeaLevel: Int, lureLevel: Int) : this(Village.SIMPLE_FISHING_BOBBER, world, luckOfTheSeaLevel, lureLevel) {
        // TODO: ADD RIVER SIZE AND TARGET BLOCK
        val entity = thrower
        this.setOwner(entity)
        this.setRodOwner(this)
        val f: Float = entity.getPitch()
        val g: Float = entity.headYaw
        val h: Float = MathHelper.cos(-g * (Math.PI.toFloat() / 180) - Math.PI.toFloat())
        val i: Float = MathHelper.sin(-g * (Math.PI.toFloat() / 180) - Math.PI.toFloat())
        val j: Float = -MathHelper.cos(-f * (Math.PI.toFloat() / 180))
        val k: Float = MathHelper.sin(-f * (Math.PI.toFloat() / 180))
        val d: Double = entity.x - i * 0.3
        val e: Double = entity.eyeY
        val l: Double = entity.z - h * 0.3
        this.refreshPositionAndAngles(d, e, l, g, f)
        var vec3d: Vec3d = Vec3d(-i.toDouble(), MathHelper.clamp(-(k / j).toDouble(), -5.0, 5.0), -h.toDouble())
        var m: Double = vec3d.length()
        vec3d = vec3d.multiply(
            0.5 / m + this.velocityRandom.nextTriangular(0.5, 0.0103365),
            0.5 / m + this.velocityRandom.nextTriangular(0.5, 0.0103365),
            0.5 / m + this.velocityRandom.nextTriangular(0.5, 0.0103365),
        )
        this.velocity = vec3d
    }

    override fun setOwner(entity: Entity?) {
        super.setOwner(entity)
    }
    private fun setRodOwner(rod: SimpleFishingBobberEntity?) {
        val entity: CustomVillagerEntity? = (this.getOwner() as CustomVillagerEntity)
        if (entity != null && entity.getProfession() != null) {
            (entity.getProfession() as Fisherman).setFishHook(rod)
        }
    }

    override fun initDataTracker() {}

    override fun shouldRender(distance: Double): Boolean {
        return distance < 2048.0
    }

    public fun pullBack() {
        (owner as CustomVillagerEntity).setActing(false)
        remove(RemovalReason.DISCARDED)
    }

    override fun updateTrackedPositionAndAngles(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, interpolationSteps: Int) {}

    override fun tick() {
        super.tick()
        if (fishingTicks >= 300) {
            this.pullBack()
            return
        }
        val owner = this.getOwner()
        if (owner == null) {
            LOGGER.info("TRIGGER REMOVAL")
            remove(RemovalReason.DISCARDED)
            return
        } else {
            (owner as CustomVillagerEntity).lookControl.lookAt(this.x, this.y, this.z, 30.0f, 30.0f)
        }
        if (isOnGround) {
            removalTimer++
            if (removalTimer >= 50) {
                LOGGER.info("TRIGGER REMOVAL TIMEOUT")
                this.pullBack()
                return
            }
        } else {
            removalTimer = 0
        }
        var f = 0.0f
        val blockPos: BlockPos = blockPos
        val fluidState: FluidState = world.getFluidState(blockPos)
        if (fluidState.isIn(FluidTags.WATER)) {
            f = fluidState.getHeight(world, blockPos)
        }
        val bl: Boolean = f > 0.0f
        if (state == BobberState.FLYING) {
            if (bl) {
                velocity = velocity.multiply(0.15, 0.2, 0.15)
                state = BobberState.BOBBING
                return
            }
            checkForCollision()
        } else {
            if (state == BobberState.BOBBING) {
                val vec3d: Vec3d = velocity
                var d: Double = y + vec3d.y - blockPos.y - f
                if (MathHelper.abs(d.toFloat()) < 0.01) {
                    d += Math.signum(d) * 0.1
                }
                fishingTicks++
                velocity = Vec3d(vec3d.x * 0.9, vec3d.y - d * random.nextFloat() * 0.2, vec3d.z * 0.9)
            }
        }
        if (!fluidState.isIn(FluidTags.WATER)) {
            velocity = velocity.add(0.0, -0.03, 0.0)
        }
        move(MovementType.SELF, velocity)
        if (state == BobberState.FLYING && (isOnGround || horizontalCollision)) {
            velocity = Vec3d.ZERO
        }
        velocity = velocity.multiply(0.92)
        refreshPosition()
    }

    private fun checkForCollision() {
        val hitResult: HitResult = ProjectileUtil.getCollision(this) { super.canHit(it) }
        onCollision(hitResult)
    }

    override fun canHit(entity: Entity?): Boolean {
        return false
        // super.canHit(entity)
    }

    override fun onEntityHit(entityHitResult: EntityHitResult) {
        // super.onEntityHit(entityHitResult)
    }

    override fun onBlockHit(blockHitResult: BlockHitResult) {
        super.onBlockHit(blockHitResult)
        velocity = velocity.normalize().multiply(blockHitResult.squaredDistanceTo(this))
        LOGGER.info("I HIT WATER")
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {}

    override fun readCustomDataFromNbt(nbt: NbtCompound) {}

    fun use(usedItem: ItemStack): Int {
        if (world.isClient || this.getOwner() == null) {
            return 0
        }
        var i = 0
        if (isOnGround) {
            i = 2
        }
        remove(RemovalReason.DISCARDED)
        return i
    }

    override fun handleStatus(status: Byte) {
        super.handleStatus(status)
    }

    override fun getMoveEffect(): Entity.MoveEffect {
        return Entity.MoveEffect.NONE
    }

    override fun remove(reason: Entity.RemovalReason?) {
        val ent = this.getOwner()
        if (ent != null) {
            this.setRodOwner(null)
        }
        super.remove(reason)
    }

    override fun onRemoved() {
        val ent = this.getOwner()
        if (ent != null) {
            this.setRodOwner(null)
        }
    }

    override fun canUsePortals(): Boolean {
        return false
    }

    override fun createSpawnPacket(): Packet<ClientPlayPacketListener> {
        val entity: Entity? = this.getOwner()
        return EntitySpawnS2CPacket(this, entity?.id ?: id)
    }

    override fun onSpawnPacket(packet: EntitySpawnS2CPacket) {
        super.onSpawnPacket(packet)
        if (this.getOwner() == null) {
            kill()
        }
    }

    enum class BobberState {
        FLYING,
        BOBBING,
    }

    enum class PositionType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID,
    }
}
