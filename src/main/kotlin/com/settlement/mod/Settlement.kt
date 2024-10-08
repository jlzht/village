package com.settlement.mod

import com.settlement.mod.command.ModCommands
import com.settlement.mod.entity.projectile.SimpleFishingBobberEntity
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.item.ModItems
import com.settlement.mod.screen.Response
import com.settlement.mod.screen.TradingScreenHandler
import com.settlement.mod.world.SettlementManager
import com.settlement.mod.world.event.HandBellUsageCallback
import com.settlement.mod.world.event.SettlementInteractionCallback
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

const val MODID = "settlement"
val LOGGER = LoggerFactory.getLogger(MODID)

object Settlement : ModInitializer {
    val VILLAGER: EntityType<AbstractVillagerEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier(MODID, "villager"),
            FabricEntityTypeBuilder
                .create<AbstractVillagerEntity>(SpawnGroup.CREATURE, { type, world -> AbstractVillagerEntity(type, world) })
                .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                .build(),
        )

    val SIMPLE_FISHING_BOBBER: EntityType<SimpleFishingBobberEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier(MODID, "simple_fishing_bobber"),
            FabricEntityTypeBuilder
                .create<SimpleFishingBobberEntity>(
                    SpawnGroup.MISC,
                    { type, world -> SimpleFishingBobberEntity(type, world, 0, 0) },
                ).disableSaving()
                .disableSummon()
                .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                .trackRangeBlocks(4)
                .trackedUpdateRate(10)
                .build(),
        )

    val TRADING_SCREEN_HANDLER: ScreenHandlerType<TradingScreenHandler> =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier(MODID, "villager_trading"),
            ExtendedScreenHandlerType({ syncId, playerInventory, buf -> TradingScreenHandler(syncId, playerInventory, buf) }),
        )

    override fun onInitialize() {
        ModCommands.register()
        ModItems.register()
        FabricDefaultAttributeRegistry.register(VILLAGER, AbstractVillagerEntity.createCustomVillagerAttributes())
        // TODO: create a file to handle events, like what I did to WorldRenderEvents
        SettlementInteractionCallback.EVENT.register({ player, pos, name ->
            SettlementManager.getInstance().addSettlement(name, pos, player)
            return@register ActionResult.PASS
        })

        HandBellUsageCallback.EVENT.register({ player, pos ->
            val manager = SettlementManager.getInstance()
            val settlements = manager.getSettlements()

            val entry = player.world.getDimensionEntry()
            SettlementManager.getDimensionString(entry)?.let { dim ->
                settlements.filter { it.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f && it.dim == dim }.firstOrNull()?.let { settlement ->
                    // .find { it.allies.map { it.uuid }.contains(player.getUuid()) }?.let {
                    LOGGER.info("Settlement: {}", settlement.name)
                    settlement.createStructure(pos, player)
                    return@register ActionResult.SUCCESS
                } ?: run {
                    Response.NO_SETTLEMENT_NEARBY.send(player)
                    return@register ActionResult.PASS
                }
            } // throw error
            return@register ActionResult.PASS
            // Response.NOT_ENOUGHT_REPUTATION.send(player)
            // return@register ActionResult.PASS
        })
    }
}
