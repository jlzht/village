package com.village.mod.client.render.entity

import com.village.mod.MODID
import com.village.mod.VillageClient
import com.village.mod.client.render.entity.model.CustomVillagerEntityModel
import com.village.mod.entity.village.CustomVillagerEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.entity.BipedEntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer
import net.minecraft.client.render.entity.model.BipedEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
class CustomVillagerEntityRenderer(ctx: EntityRendererFactory.Context) :
    BipedEntityRenderer<CustomVillagerEntity, BipedEntityModel<CustomVillagerEntity>>
        (ctx, CustomVillagerEntityModel(ctx.getPart(VillageClient.VILLAGER_LAYER)), 0.5F) {
    init {
        this.addFeature(HeadFeatureRenderer(this, ctx.modelLoader, ctx.heldItemRenderer))
        this.addFeature(HeldItemFeatureRenderer(this, ctx.heldItemRenderer))
        this.addFeature(
            ArmorFeatureRenderer(
                this,
                CustomVillagerEntityModel(ctx.getPart(VillageClient.VILLAGER_ARMOR_INNER)),
                CustomVillagerEntityModel(ctx.getPart(VillageClient.VILLAGER_ARMOR_OUTER)),
                ctx.getModelManager(),
            ),
        )
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
