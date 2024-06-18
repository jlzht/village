package com.village.mod

import com.village.mod.client.gui.screen.TradingScreen
// import com.village.mod.client.item.VillageModelPredicateProviders
import com.village.mod.client.render.entity.SimpleFishingBobberEntityRenderer
import com.village.mod.client.render.entity.CustomVillagerEntityRenderer
import com.village.mod.client.render.entity.model.CustomVillagerEntityModel
import com.village.mod.client.render.debug.VillageGraphDebugRenderer
import com.village.mod.client.render.debug.DebugGraph
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.util.Identifier
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import com.village.mod.network.NodeGraphPacket

@Environment(EnvType.CLIENT)
object VillageClient : ClientModInitializer {
    private val debugGraph = mutableMapOf<DebugGraph, MutableList<DebugGraph>>()
    val VILLAGER_LAYER = EntityModelLayer(Identifier(MODID, "villager"), "main")
    val VILLAGER_ARMOR_OUTER = EntityModelLayer(Identifier(MODID, "armor_outer"), "main")
    val VILLAGER_ARMOR_INNER = EntityModelLayer(Identifier(MODID, "armor_inner"), "main")

    override fun onInitializeClient() {
        EntityRendererRegistry.register(Village.SIMPLE_FISHING_BOBBER, { context -> SimpleFishingBobberEntityRenderer(context) })
        EntityRendererRegistry.register(Village.VILLAGER, { context -> CustomVillagerEntityRenderer(context) })
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_LAYER, CustomVillagerEntityModel.Companion::getTexturedModelData)
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_ARMOR_INNER, CustomVillagerEntityModel.Companion::getInnerArmorLayer)
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_ARMOR_OUTER, CustomVillagerEntityModel.Companion::getOuterArmorLayer)
        HandledScreens.register(Village.TRADING_SCREEN_HANDLER, { handler, inventory, title -> TradingScreen(handler, inventory, title) })
        WorldRenderEvents.END.register(VillageGraphDebugRenderer(debugGraph))
        ClientPlayNetworking.registerGlobalReceiver(NodeGraphPacket.ID) { client, _, buf, _ ->
            val packet = NodeGraphPacket.decode(buf)
            client.execute {
                packet.graph.forEach { (key, value) ->
                    debugGraph[key] = value.toMutableList()
                }
            }
        }
        // VillageModelPredicateProviders.registerModelPredicateProviders()
    }
}
