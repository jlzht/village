package com.village.mod.client.render.entity.model

import com.village.mod.MODID
import com.village.mod.LOGGER
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.villager.State
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.model.Dilation
import net.minecraft.client.model.ModelData
import net.minecraft.client.model.ModelPart
import net.minecraft.client.model.ModelPartBuilder
import net.minecraft.client.model.ModelPartData
import net.minecraft.client.model.ModelTransform
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.CrossbowPosing
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.client.render.entity.model.EntityModelPartNames
import net.minecraft.client.render.entity.model.ModelWithArms
import net.minecraft.client.render.entity.model.ModelWithHead
import net.minecraft.client.render.entity.model.SinglePartEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.Items
import net.minecraft.util.Arm
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

@Environment(EnvType.CLIENT)
class CustomVillagerEntityModel(private val root: ModelPart) : SinglePartEntityModel<CustomVillagerEntity>(), ModelWithHead,ModelWithArms {
    private var offsets = false
    private val head: ModelPart = root.getChild(EntityModelPartNames.HEAD)
    private val body: ModelPart = root.getChild(EntityModelPartNames.BODY)
    private val nose: ModelPart = head.getChild(EntityModelPartNames.NOSE)
    private val leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG)
    private val rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG)
    private val leftArm = root.getChild(EntityModelPartNames.LEFT_ARM)
    private val rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM)

    companion object {
        val layer: EntityModelLayer = EntityModelLayer(Identifier(MODID, "villager"), "main")

        fun getTexturedModelData(): TexturedModelData {
            var modelData: ModelData = ModelData()
            var modelPartData: ModelPartData = modelData.getRoot()
            var head: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.HEAD,
                    ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                    ModelTransform.NONE,
                )
            var nose: ModelPartData =
                head.addChild(
                    EntityModelPartNames.NOSE,
                    ModelPartBuilder.create().uv(24, 0).cuboid(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F, Dilation(0.0F)),
                    ModelTransform.pivot(0.0F, -2.0F, 0.0F),
                )
            var body: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.BODY,
                    ModelPartBuilder.create().uv(
                        16,
                        20,
                    ).cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F).uv(0, 38).cuboid(-4.0f, 0.0f, -3.0f, 8.0f, 20.0f, 6.0f, Dilation(0.5F)),
                    ModelTransform.pivot(0.0F, 0.0F, 0.0F),
                )
            var leftLeg: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.LEFT_LEG,
                    ModelPartBuilder.create().uv(0, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation(0.0F)),
                    ModelTransform.pivot(2.0F, 12.0F, 0.0F),
                )
            var rightLeg: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.RIGHT_LEG,
                    ModelPartBuilder.create().uv(
                        0,
                        22,
                    ).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation(0.0F)).mirrored(false),
                    ModelTransform.pivot(-2.0F, 12.0F, 0.0F),
                )
            var rightArm: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.RIGHT_ARM,
                    ModelPartBuilder.create().uv(44, 22).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation(0.0F)),
                    ModelTransform.pivot(-5.0F, 2.0F, 0.0F),
                )
            var leftArm: ModelPartData =
                modelPartData.addChild(
                    EntityModelPartNames.LEFT_ARM,
                    ModelPartBuilder.create().uv(
                        44,
                        22,
                    ).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation(0.0F)).mirrored(false),
                    ModelTransform.pivot(5.0F, 2.0F, 0.0F),
                )
            return TexturedModelData.of(modelData, 64, 64)
        }
    }

    private fun getArm(arm: Arm): ModelPart {
        return if (arm == Arm.LEFT) leftArm else rightArm
    }

    override fun getHead(): ModelPart {
        return head
    }

    override fun setArmAngle(
        arm: Arm,
        matrices: MatrixStack,
    ) {
        getArm(arm).rotate(matrices)
    }

    override fun setAngles(entity: CustomVillagerEntity, f: Float, g: Float, h: Float, i: Float, j: Float) {
        this.head.yaw = i * ((Math.PI / 180).toFloat())
        this.head.pitch = j * ((Math.PI / 180).toFloat())
        this.body.yaw = 0.0f
        this.rightArm.pivotZ = 0.0f
        this.rightArm.pivotX = -5.0f
        this.leftArm.pivotZ = 0.0f
        this.leftArm.pivotX = 5.0f
        if ((entity).isState(State.SIT)) {
            rightArm.pitch = -0.62831855f
            rightArm.yaw = 0.0f
            rightArm.roll = 0.0f
            leftArm.pitch = -0.62831855f
            leftArm.yaw = 0.0f
            leftArm.roll = 0.0f
            rightLeg.pitch = -1.4137167f
            rightLeg.yaw = 0.31415927f
            rightLeg.roll = 0.07853982f
            leftLeg.pitch = -1.4137167f
            leftLeg.yaw = -0.31415927f
            leftLeg.roll = -0.07853982f
            this.offsets = true
        } else {
            rightArm.pitch = MathHelper.cos(f * 0.6662f + Math.PI.toFloat()) * 2.0f * g * 0.5f
            rightArm.yaw = 0.0f
            rightArm.roll = 0.0f
            leftArm.pitch = MathHelper.cos(f * 0.6662f) * 2.0f * g * 0.5f
            leftArm.yaw = 0.0f
            leftArm.roll = 0.0f
            rightLeg.pitch = MathHelper.cos(f * 0.6662f) * 1.4f * g * 0.5f
            rightLeg.yaw = 0.0f
            rightLeg.roll = 0.0f
            leftLeg.pitch = MathHelper.cos(f * 0.6662f + Math.PI.toFloat()) * 1.4f * g * 0.5f
            leftLeg.yaw = 0.0f
            leftLeg.roll = 0.0f
            this.offsets = false
        }
        if (entity.isState(State.ATTACK)) {
            if (entity.isHolding(Items.CROSSBOW)) {
                if (entity.isCharging()) {
                    CrossbowPosing.charge(rightArm, leftArm, entity, true)
                } else {
                    CrossbowPosing.hold(rightArm, leftArm, head, true)
                }
            }
            if (entity.isHolding(Items.BOW)) {
                this.rightArm.yaw = -0.1f + this.head.yaw
                this.leftArm.yaw = 0.1f + this.head.yaw + 0.4f
                this.rightArm.pitch = -1.5707964f + this.head.pitch
                this.leftArm.pitch = -1.5707964f + this.head.pitch
            }
        } else if (entity.isHoldingTool()) {
            this.rightArm.pitch = this.rightArm.pitch * 0.5f - 0.31415927f
            this.rightArm.yaw = 0.0f
        }
        this.animateArms(entity, h)
    }

    private fun getPreferredArm(entity: CustomVillagerEntity): Arm {
        return entity.getMainArm()
    }

    protected fun animateArms(entity: CustomVillagerEntity, animationProgress: Float) {
        if (this.handSwingProgress <= 0.0f) {
            return
        }
        val arm = getPreferredArm(entity)
        val modelPart = getArm(entity.getMainArm())
        var f = this.handSwingProgress
        this.body.yaw = MathHelper.sin(MathHelper.sqrt(f) * (Math.PI.toFloat() * 2)) * 0.2f
        if (arm == Arm.LEFT) {
            this.body.yaw *= -1.0f
        }
        this.rightArm.pivotZ = MathHelper.sin(this.body.yaw) * 5.0f
        this.rightArm.pivotX = -MathHelper.cos(this.body.yaw) * 5.0f
        this.leftArm.pivotZ = -MathHelper.sin(this.body.yaw) * 5.0f
        this.leftArm.pivotX = MathHelper.cos(this.body.yaw) * 5.0f
        this.rightArm.yaw += this.body.yaw
        this.leftArm.yaw += this.body.yaw
        this.leftArm.pitch += this.body.yaw
        f = 1.0f - this.handSwingProgress
        f *= f
        f *= f
        f = 1.0f - f
        val g = MathHelper.sin(f * Math.PI.toFloat())
        val h = (MathHelper.sin(this.handSwingProgress * Math.PI.toFloat()) * -(this.head.pitch - 0.7f) * 0.75f)
        modelPart.pitch -= g * 1.2f + h
        modelPart.yaw += this.body.yaw * 2.0f
        modelPart.roll += MathHelper.sin(this.handSwingProgress * Math.PI.toFloat()) * -0.4f
    }

    override fun render(
        matrices: MatrixStack,
        vertices: VertexConsumer,
        light: Int,
        overlay: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        if (offsets) {
            matrices.translate(0.0, 0.6, 0.0)
        }
        super.render(matrices, vertices, light, overlay, r, g, b, a)
    }

    override fun getPart(): ModelPart {
        return this.root
    }
}
