package com.village.mod

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.VillageItems
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.structure.Building
import com.village.mod.village.structure.Farm
import com.village.mod.village.structure.Pond
import com.village.mod.world.VillageSaverAndLoader
import com.village.mod.world.event.HandBellUsageCallback
import com.village.mod.world.event.VillageInteractionCallback
import com.village.mod.world.event.VillagerKilledCallback
import com.village.mod.world.event.VillagerRequestCallback
import com.village.mod.world.event.VillagerSpawnedCallback
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PointOfInterestHelper
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.block.BarrelBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.entity.BellBlockEntity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
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
    val TEST: Set<BlockState> = listOf(
        Blocks.ANVIL,
        Blocks.JUKEBOX,
    ).flatMap { block -> block.stateManager.states }
        .toSet()

    val BLACKSMITH: PointOfInterestType = PointOfInterestHelper.register(Identifier(MODID, "blacksmith"), 2, 8, TEST)

    override fun onInitialize() {
        VillageItems.register()
        FabricDefaultAttributeRegistry.register(VILLAGER, CustomVillagerEntity.createCustomVillagerAttributes())

        VillageInteractionCallback.EVENT.register({ player, pos, name ->
            val manager = VillageSaverAndLoader.getServerState(player.world.getServer()!!)
            for (village in manager.villages) {
                if (village.pos == pos) {
                    player.sendMessage(Text.translatable("block.village.bell.interaction.full"), true)
                    return@register ActionResult.FAIL
                }
                if (village.name == name) {
                    player.sendMessage(Text.translatable("block.village.bell.interaction.same"), true)
                    return@register ActionResult.FAIL
                }
                if (village.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                    player.sendMessage(Text.translatable("block.village.bell.interaction.near"), true)
                    return@register ActionResult.FAIL
                }
            }
            manager.addVillage(name, pos)
            player.sendMessage(Text.translatable("block.village.bell.interaction.new").append(Text.translatable(name)), true)
            return@register ActionResult.PASS
        })

        VillagerSpawnedCallback.EVENT.register({ entity ->
            entity.world.getServer()?.let { server ->
                VillageSaverAndLoader.getServerState(server).villages
                    .filter { it.isLoaded }.firstOrNull()?.let {
                        it.addVillager(entity)
                        return@register ActionResult.SUCCESS
                    }
            }
            return@register ActionResult.FAIL
        })

        VillagerKilledCallback.EVENT.register({ entity ->
            entity.world.getServer()?.let { server ->
                VillageSaverAndLoader.getServerState(server).villages
                    .find { it.id == entity.villageKey }?.let {
                        it.removeVillager(entity.key)
                    }
            }
            return@register ActionResult.PASS
        })

        VillagerRequestCallback.EVENT.register({ entity, type ->
            entity.world.getServer()?.let { server ->
                VillageSaverAndLoader.getServerState(server).villages.find { it.id == entity.villageKey }?.let { village ->
                    village.villagers.getValue(entity.key)?.let {
                        // if (villager != null) {
                        if (type.ordinal >= 5) {
                            village.assignStructureToVillager(entity.key, type, entity.homeStructure)
                        } else {
                            village.assignStructureToVillager(entity.key, type, entity.workStructure)
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })

        HandBellUsageCallback.EVENT.register({ player, pos, world ->
            world.getServer()?.let { server ->
                VillageSaverAndLoader.getServerState(server).villages.forEach { village ->
                    // TODO:
                    // - Make village range be relative to existing structures
                    // - Instead of iterate thru all villages, check for player relation with village | Create a list of UUIDs in village type |
                    if (village.isLoaded && village.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        val block = world.getBlockState(pos).getBlock()
                        when (block) {
                            is FarmlandBlock -> {
                                if (!village.isStructureInRegion(pos)) {
                                    Farm.createStructure(pos, player)?.let { farm ->
                                        village.addStructure(farm)
                                        player.sendMessage(Text.translatable("item.village.hand_bell.farm.added"), true)
                                        return@register ActionResult.SUCCESS
                                    }
                                }
                                player.sendMessage(Text.translatable("item.village.hand_bell.farm.occupied"), true)
                                return@register ActionResult.FAIL
                            }
                            is BarrelBlock -> {
                                if (village.isStructureIsRange(8.0f)) {
                                    Pond.createStructure(pos, player)?.let { pond ->
                                        village.addStructure(pond)
                                        player.sendMessage(Text.translatable("item.village.hand_bell.pond.added"), true)
                                        return@register ActionResult.SUCCESS
                                    }
                                }
                                player.sendMessage(Text.translatable("item.village.hand_bell.pond.occupied"), true)
                                return@register ActionResult.FAIL
                            }
                            is DoorBlock -> {
                                if (!village.isStructureInRegion(pos)) {
                                    val direction = world.getBlockState(pos).get(DoorBlock.FACING)
                                    val r = player.blockPos.getSquaredDistance(pos.offset(direction, 1).toCenterPos())
                                    val l = player.blockPos.getSquaredDistance(pos.offset(direction.getOpposite(), 1).toCenterPos())
                                    Building.createStructure(if (r > l) { pos.offset(direction, 1) } else { pos.offset(direction.getOpposite()) }, player)?.let { building ->
                                        village.addStructure(building)
                                        player.sendMessage(Text.translatable("item.village.hand_bell.building.added"), true)
                                        return@register ActionResult.SUCCESS
                                    }
                                } else {
                                    player.sendMessage(Text.translatable("item.village.hand_bell.building.occupied"), true)
                                }
                                return@register ActionResult.FAIL
                            }
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })

        ServerEntityEvents.ENTITY_LOAD.register({ entity, serverWorld ->
            if (entity is CustomVillagerEntity) {
                VillageSaverAndLoader.getServerState(serverWorld.getServer()).villages
                    .find { it.id == entity.villageKey }?.let { village ->
                        if (entity.key != -1) {
                            village.villagers[entity.key] = entity
                            LOGGER.info("| Villager({}) from village: {}, and profession: {}", entity.key, village.name, entity.getProfession()?.type)
                        } else {
                            entity.villageKey = -1
                            LOGGER.info("ORPHANED")
                        }
                    }
            }
        })
        // TODO: maybe just check chunks maybe? which is faster? on spawn signal to village lol
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                VillageSaverAndLoader.getServerState(serverWorld.getServer()).villages.find { it.pos == blockEntity.getPos() }?.let { it.isLoaded = true }
                // val manager = VillageSaverAndLoader.getServerState(serverWorld.getServer())
                // for (village in manager.villages) {
                //    if (village.pos == blockEntity.getPos()) {
                //        village.isLoaded = true
                //        LOGGER.info("VILLAGE: {} LOADED", village.name)
                //        LOGGER.info("STRUCTURES: {}", village.structures)
                //    }
                // }
            }
        })
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                VillageSaverAndLoader.getServerState(serverWorld.getServer()).villages.find { it.pos == blockEntity.getPos() }?.let { it.isLoaded = false }
                // for (village in manager.villages) {
                //    if (village.pos == blockEntity.getPos()) {
                //        village.isLoaded = false
                //        LOGGER.info("VILLAGE: {} LOADED", village.name)
                //        LOGGER.info("STRUCTURES: {}", village.structures)
                //    }
                // }
            }
        })
    }
}
