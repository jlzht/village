package com.village.mod.client.render.entity

import com.village.mod.MODID
import com.village.mod.entity.village.VigerEntity
import com.village.mod.client.render.entity.model.VigerModel
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
class VigerRenderer(context: EntityRendererFactory.Context) :
    MobEntityRenderer<VigerEntity, VigerModel>
    (context, VigerModel(context.getPart(VigerModel.layer)), 0.5F) {
    init {
        addFeature(HeadFeatureRenderer(this, context.modelLoader, context.heldItemRenderer))
    }

    private val TEXTURE = Identifier(MODID, "textures/entity/villager.png")

    override fun getTexture(entity: VigerEntity): Identifier {
        return TEXTURE
    }

    override fun scale(
        entity: VigerEntity,
        matrixStack: MatrixStack,
        f: Float,
    ) {
        var g: Float = 0.9375f
        matrixStack.scale(0.9375f, 0.9375f, 0.9375f)
    }
}
