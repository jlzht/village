//package com.village.mod.client.render.entity.feature
//
//import com.village.mod.client.render.entity.model.CustomVillagerEntityModel
//import com.village.mod.entity.village.CustomVillagerEntity
//import net.fabricmc.api.EnvType
//import net.fabricmc.api.Environment
//import net.minecraft.client.render.VertexConsumerProvider
//import net.minecraft.client.render.entity.feature.FeatureRenderer
//import net.minecraft.client.render.entity.feature.FeatureRendererContext
//import net.minecraft.client.render.entity.model.EntityModelLayers
//import net.minecraft.client.render.entity.model.EntityModelLoader
//import net.minecraft.client.render.entity.model.HorseEntityModel
//import net.minecraft.client.util.math.MatrixStack
//import com.village.mod.VillageClient
//
//@Environment(EnvType.CLIENT)
//class AbstractVillagerArmorFeatureRenderer(
//    context: FeatureRendererContext<CustomVillagerEntity, CustomVillagerEntityModel<CustomVillagerEntity>>,
//    loader: EntityModelLoader,
//) : FeatureRenderer<CustomVillagerEntity, CustomVillagerEntityModel<CustomVillagerEntity>>(context) {
//    private val model: CustomVillagerEntityModel<CustomVillagerEntity> = CustomVillagerEntityModel(loader.getModelPart(EntityModelLayers.HORSE_ARMOR))
//
//    override fun render(
//        matrixStack: MatrixStack,
//        vertexConsumerProvider: VertexConsumerProvider,
//        i: Int,
//        villagerEntity: CustomVillagerEntity,
//        f: Float,
//        g: Float,
//        h: Float,
//        j: Float,
//        k: Float,
//        l: Float,
//    ) {
//    }
//}
