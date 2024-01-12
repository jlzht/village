package com.village.mod.client.render.entity.model

import com.village.mod.MODID
import com.village.mod.entity.village.VigerEntity
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
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.client.render.entity.model.EntityModelPartNames
import net.minecraft.client.render.entity.model.ModelWithArms
import net.minecraft.client.render.entity.model.ModelWithHead
import net.minecraft.client.render.entity.model.SinglePartEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Arm
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

@Environment(EnvType.CLIENT)
class VigerModel(var root: ModelPart) : SinglePartEntityModel<VigerEntity>(), ModelWithArms, ModelWithHead {
    private var offsets: Boolean = false
    private val head: ModelPart = root.getChild(EntityModelPartNames.HEAD)
    private val body: ModelPart = root.getChild(EntityModelPartNames.BODY)
    private val nose: ModelPart = head.getChild(EntityModelPartNames.NOSE)
    private val leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG)
    private val rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG)
    private val leftArm = root.getChild(EntityModelPartNames.LEFT_ARM)
    private val rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM)

    companion object {
        val layer: EntityModelLayer = EntityModelLayer(Identifier(MODID, "viger"), "main")

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

    override fun setAngles(
        entity: VigerEntity,
        f: Float,
        g: Float,
        h: Float,
        i: Float,
        j: Float,
    ) {
        head.yaw = i * ((Math.PI / 180).toFloat())
        head.pitch = j * ((Math.PI / 180).toFloat())
        if (riding || entity.isSitting()) {
            this.offsets = true
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
        } else {
            this.offsets = false
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
        }
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
        return root
    }
}
