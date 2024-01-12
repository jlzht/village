package com.village.mod

import com.village.mod.client.gui.screen.TradingScreen
import com.village.mod.client.render.entity.VigerRenderer
import com.village.mod.client.render.entity.model.VigerModel
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens

@Environment(EnvType.CLIENT)
object VillageClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(Village.VIGER, { context -> VigerRenderer(context) })
        EntityModelLayerRegistry.registerModelLayer(VigerModel.layer, VigerModel.Companion::getTexturedModelData)
        HandledScreens.register(Village.TRADING_SCREEN_HANDLER, { handler, inventory, title -> TradingScreen(handler, inventory, title) })
    }
}
