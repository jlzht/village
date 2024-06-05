package com.village.mod

import com.village.mod.client.gui.screen.TradingScreen
// import com.village.mod.client.item.VillageModelPredicateProviders
import com.village.mod.client.render.entity.SimpleFishingBobberEntityRenderer
import com.village.mod.client.render.entity.CustomVillagerEntityRenderer
import com.village.mod.client.render.entity.model.CustomVillagerEntityModel
import com.village.mod.client.render.debug.VillageGraphDebugRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.util.Identifier
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf

@Environment(EnvType.CLIENT)
object VillageClient : ClientModInitializer {
    private val nodes = mutableListOf<Vec3d>()
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
        WorldRenderEvents.END.register(VillageGraphDebugRenderer(nodes))
        ClientPlayNetworking.registerGlobalReceiver(NodesPacket.ID) { client, _, buf, _ ->
            val packet = NodesPacket.decode(buf)
            client.execute {
                LOGGER.info("I AM HERE!")
                nodes.clear()
                nodes.addAll(packet.positions)
            }
        }

        // VillageModelPredicateProviders.registerModelPredicateProviders()
    }
}

data class NodesPacket(val positions: List<Vec3d>) {
    companion object {
        val ID = Identifier(MODID, "village_graph")

        fun encode(packet: NodesPacket, buf: PacketByteBuf) {
            buf.writeInt(packet.positions.size)
            packet.positions.forEach { pos ->
                buf.writeVec3d(pos)
            }
        }

        fun decode(buf: PacketByteBuf): NodesPacket {
            val size = buf.readInt()
            val positions = mutableListOf<Vec3d>()
            repeat(size) {
                positions.add(buf.readVec3d())
            }
            return NodesPacket(positions)
        }

        fun sendToClient(player: ServerPlayerEntity, positions: List<Vec3d>) {
            val buf = PacketByteBufs.create()
            encode(NodesPacket(positions), buf)
            ServerPlayNetworking.send(player, ID, buf)
        }
    }
}
