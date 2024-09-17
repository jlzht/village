package com.settlement.mod

import com.settlement.mod.client.gui.screen.TradingScreen
import com.settlement.mod.client.render.debug.SettlementDebugRenderer
// import com.settlement.mod.client.item.VillageModelPredicateProviders
import com.settlement.mod.client.render.entity.SimpleFishingBobberEntityRenderer
import com.settlement.mod.client.render.entity.AbstractVillagerEntityRenderer
import com.settlement.mod.client.render.entity.model.AbstractVillagerEntityModel
import com.settlement.mod.network.SettlementDebugDataPacket
import com.settlement.mod.network.SettlementDebugData
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.util.Identifier
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screen.ingame.HandledScreens

@Environment(EnvType.CLIENT)
object SettlementClient : ClientModInitializer {
    private val debugData = mutableListOf<SettlementDebugData>()
    val VILLAGER_LAYER = EntityModelLayer(Identifier(MODID, "villager"), "main")
    val VILLAGER_ARMOR_OUTER = EntityModelLayer(Identifier(MODID, "armor_outer"), "main")
    val VILLAGER_ARMOR_INNER = EntityModelLayer(Identifier(MODID, "armor_inner"), "main")

    override fun onInitializeClient() {
        EntityRendererRegistry.register(Settlement.SIMPLE_FISHING_BOBBER, { context -> SimpleFishingBobberEntityRenderer(context) })
        EntityRendererRegistry.register(Settlement.VILLAGER, { context -> AbstractVillagerEntityRenderer(context) })
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_LAYER, AbstractVillagerEntityModel.Companion::getTexturedModelData)
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_ARMOR_INNER, AbstractVillagerEntityModel.Companion::getInnerArmorLayer)
        EntityModelLayerRegistry.registerModelLayer(VILLAGER_ARMOR_OUTER, AbstractVillagerEntityModel.Companion::getOuterArmorLayer)
        HandledScreens.register(Settlement.TRADING_SCREEN_HANDLER, { handler, inventory, title -> TradingScreen(handler, inventory, title) })
        WorldRenderEvents.END.register(SettlementDebugRenderer(debugData))
        ClientPlayNetworking.registerGlobalReceiver(SettlementDebugDataPacket.ID) { client, _, buf, _ ->
            val packet = SettlementDebugDataPacket.decode(buf)
            debugData.clear()
            client.execute {
                packet.data.forEach { data ->
                    debugData.add(data)
                }
            }
        }

        // VillageModelPredicateProviders.registerModelPredicateProviders()
    }
}
