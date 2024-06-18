package com.village.mod

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.item.VillageItems
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.villager.Action
import com.village.mod.world.Settlement
import com.village.mod.screen.Response
import com.village.mod.world.SettlementLoader
import com.village.mod.world.event.HandBellUsageCallback
import com.village.mod.world.event.VillageInteractionCallback
import com.village.mod.world.event.VillagerKilledCallback
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.world.event.VillagerSpawnedCallback
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PointOfInterestHelper
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BellBlockEntity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.world.poi.PointOfInterestType
import org.slf4j.LoggerFactory

const val MODID = "village"
val LOGGER = LoggerFactory.getLogger(MODID)

object Village : ModInitializer {
    val VILLAGER: EntityType<CustomVillagerEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier(MODID, "villager"),
            FabricEntityTypeBuilder.create<CustomVillagerEntity>(SpawnGroup.CREATURE, { type, world -> CustomVillagerEntity(type, world) })
                .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build(),
        )

    val SIMPLE_FISHING_BOBBER: EntityType<SimpleFishingBobberEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier(MODID, "simple_fishing_bobber"),
            FabricEntityTypeBuilder.create<SimpleFishingBobberEntity>(SpawnGroup.MISC, { type, world -> SimpleFishingBobberEntity(type, world, 0, 0) })
                .disableSaving()
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

    // TODO: register more POIs
    val TEST: Set<BlockState> = setOf(
        Blocks.ANVIL,
        Blocks.JUKEBOX,
    ).flatMap { block -> block.stateManager.states }
        .toSet()

    val BLACKSMITH: PointOfInterestType = PointOfInterestHelper.register(Identifier(MODID, "blacksmith"), 2, 8, TEST)
    lateinit var settlements: MutableList<Settlement>

    override fun onInitialize() {
        VillageItems.register()
        FabricDefaultAttributeRegistry.register(VILLAGER, CustomVillagerEntity.createCustomVillagerAttributes())
        // TODO: create a file to handle events, like what I did to WorldRenderEvents
        VillageInteractionCallback.EVENT.register({ player, pos, name ->
            player.world.getServer()?.let {
                val manager = SettlementLoader.getServerState(it)
                for (village in manager.villages) {
                    if (village.pos == pos) {
                        Response.PLACE_IS_SETTLEMENT_ALREADY.send(player)
                        return@register ActionResult.FAIL
                    }
                    if (village.name == name) {
                        Response.ANOTHER_SETTLEMENT_HAS_NAME.send(player)
                        return@register ActionResult.FAIL
                    }
                    if (village.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        Response.ANOTHER_SETTLEMENT_NEARBY.send(player)
                        return@register ActionResult.FAIL
                    }
                }

                manager.addVillage(name, pos, player)
            }
            return@register ActionResult.PASS
        })

        HandBellUsageCallback.EVENT.register({ player, pos ->
            // TODO: IMEPLEMENT CHUNK OWNING | with chunk owning this filter will be useless
            settlements.filter { it.isLoaded && it.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f }.takeIf { it.isNotEmpty() }?.let {
                // it.find { it.allies.map { it.uuid }.contains(player.getUuid()) }?.let { settlement ->
                it.first {
                    it.createStructure(pos, player)
                    return@register ActionResult.SUCCESS
                }
                Response.NOT_ENOUGHT_REPUTATION.send(player)
                return@register ActionResult.FAIL
            }
            Response.NO_SETTLEMENT_NEARBY.send(player)
            return@register ActionResult.PASS
        })

        // rethink this
        VillagerSpawnedCallback.EVENT.register({ entity ->
            settlements.filter { it.isLoaded }.firstOrNull()?.let {
                it.addVillager(entity)
            }
            return@register ActionResult.PASS
        })

        VillagerKilledCallback.EVENT.register({ entity ->
            settlements.find { it.id == entity.villageKey }?.let {
                it.removeVillager(entity.key)
            }
            return@register ActionResult.PASS
        })
        // request structure
        VillagerRequestCallback.EVENT.register({ entity, type ->
            settlements.find { it.id == entity.villageKey }?.let { village ->
                village.villagers.getValue(entity.key)?.let { villager ->
                    if (entity.attachedNode == -1) {
                        village.graph.getNodes().toList().minByOrNull { entity.blockPos.getManhattanDistance(it.component2().pos) }?.let {
                            entity.attachedNode = it.component1()
                        }
                    } else {
                        village.graph.getNode(entity.attachedNode)?.let { kk ->
                            if (!kk.others.isEmpty()) {
                                val s = kk.others.random()
                                village.graph.getNode(s)?.let { ls ->
                                    entity.attachedNode = s
                                    entity.errand.push(Errand(ls.pos, Action.MOVE))
                                }
                            } else {
                            }
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })

        ServerLifecycleEvents.SERVER_STARTED.register({ server ->
            settlements = SettlementLoader.getServerState(server).villages
        })

        ServerEntityEvents.ENTITY_LOAD.register({ entity, _ ->
            if (entity is CustomVillagerEntity) {
                settlements.find { it.id == entity.villageKey }?.let { village ->
                    if (entity.key != -1) {
                        village.villagers[entity.key] = entity
                        LOGGER.info("| Villager({}) from village: {}, and profession: {}", entity.key, village.name, entity.getProfession()?.type)
                    } else {
                        entity.villageKey = -1
                        LOGGER.info("| Villager orphaned")
                    }
                }
            }
        })

        // TODO: instead of "loading" settlements in a player activates a chunk with BellBlock, make chunks be claimed by Settlement
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register({ blockEntity, _ ->
            if (blockEntity is BellBlockEntity) {
                settlements.find { it.pos == blockEntity.getPos() }?.let { it.isLoaded = true }
            }
        })
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register({ blockEntity, _ ->
            if (blockEntity is BellBlockEntity) {
                settlements.find { it.pos == blockEntity.getPos() }?.let { it.isLoaded = false }
            }
        })
    }
}
