package com.village.mod.client.render.entity

import com.village.mod.MODID
import com.village.mod.client.render.entity.model.CustomVillagerEntityModel
import com.village.mod.entity.village.CustomVillagerEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
class CustomVillagerEntityRenderer(ctx: EntityRendererFactory.Context) :
    MobEntityRenderer<CustomVillagerEntity, CustomVillagerEntityModel>
        (ctx, CustomVillagerEntityModel(ctx.getPart(CustomVillagerEntityModel.layer)), 0.35F) {
    init {
        this.addFeature(HeadFeatureRenderer(this, ctx.modelLoader, ctx.heldItemRenderer))
        this.addFeature(HeldItemFeatureRenderer(this, ctx.heldItemRenderer))
    }

    private val TEXTURE = Identifier(MODID, "textures/entity/villager.png")

    override fun getTexture(entity: CustomVillagerEntity): Identifier {
        return TEXTURE
    }

    override fun scale(entity: CustomVillagerEntity, matrixStack: MatrixStack, f: Float) {
        var g: Float = 0.9375f
        matrixStack.scale(0.9375f, 0.9375f, 0.9375f)
    }
}
