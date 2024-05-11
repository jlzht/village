package com.village.mod.village.profession

import com.village.mod.entity.ai.goal.RangedAttackGoal
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.ActiveTargetGoal
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.item.ArrowItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraft.entity.projectile.ProjectileUtil
import org.joml.Vector3f

class Guard() : Profession() {
    override val type = ProfessionType.GUARD
    override fun addProfessionTasks(worker: CustomVillagerEntity) {
        // worker.appendGoal(3, MeleeAttackGoal(worker, 1.0, true))
        worker.appendGoal(3, RangedAttackGoal(worker, 1.0))
        worker.appendTargetGoal(2, ActiveTargetGoal((worker as MobEntity), MobEntity::class.java, 5, true, false, { entity -> entity is Monster }))
        worker.appendTargetGoal(1, ActiveTargetGoal<PlayerEntity>((worker as MobEntity), PlayerEntity::class.java, true));
    }

    fun shoot(
        world: World,
        entity: CustomVillagerEntity,
        target: LivingEntity,
    ) {
        if (world.isClient) {
            return
        }
        val arrowItem: ArrowItem = Items.ARROW as ArrowItem
        var itemStack = entity.getProjectileType(entity.getStackInHand(ProjectileUtil.getHandPossiblyHolding(entity, if (entity.isHolding(Items.CROSSBOW)) {Items.CROSSBOW} else {Items.BOW} )))
        val projectileEntity: PersistentProjectileEntity = arrowItem.createArrow(world, itemStack, entity)
        if (entity.isHolding(Items.CROSSBOW)) {
          projectileEntity.setShotFromCrossbow(true)
        } else {
        }
        this.shootTo(entity, target, projectileEntity, 1.0f, 1.6f)
        world.spawnEntity(projectileEntity)
    }

    fun shootTo(entity: CustomVillagerEntity, target: LivingEntity, projectile: ProjectileEntity, multishotSpray: Float, speed: Float) {
        val d = target.getX() - entity.getX()
        val e = target.getZ() - entity.getZ()
        val f = Math.sqrt(d * d + e * e)
        val g = target.getBodyY(0.3333333333333333) - projectile.getY() + f * 0.2F
        val vector3f = getProjectileLaunchVelocity(entity, Vec3d(d, g, e), multishotSpray)
        projectile.setVelocity(vector3f.x().toDouble(), vector3f.y().toDouble(), vector3f.z().toDouble(), speed, 14 - entity.world.difficulty.id * 4.0f)
        // entity.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f / (entity.random.nextFloat() * 0.4f + 0.8f))
    }

    fun getProjectileLaunchVelocity(entity: LivingEntity, positionDelta: Vec3d, multishotSpray: Float): Vector3f {
        val vector3f = positionDelta.toVector3f().normalize()
        var vector3f2 = Vector3f(vector3f).cross(Vector3f(0.0f, 0.0f, 1.0f))
        if (vector3f2.lengthSquared() <= 1.0E-7) {
            val vec3d = entity.getOppositeRotationVector(1.0f)
            vector3f2 = Vector3f(vector3f).cross(vec3d.toVector3f())
        }
        // val vector3f3 = vector3f.rotateAxis(1.5707964f, vector3f2.x, vector3f2.y, vector3f2.z)
        val vector3f3 = vector3f.rotateAxis(0.0f, vector3f2.x, vector3f2.y, vector3f2.z)
        return vector3f.rotateAxis((Math.PI.toFloat() / 180), vector3f3.x, vector3f3.y, vector3f3.z)
    }
}
