package com.village.mod

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.VillageItems
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.world.Data
import com.village.mod.world.Farm
import com.village.mod.world.Home
import com.village.mod.world.Point
import com.village.mod.world.Pond
import com.village.mod.world.Region
import com.village.mod.world.StateSaverAndLoader
import com.village.mod.world.VillageData
import com.village.mod.world.VillageUtils
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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.block.BarrelBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.entity.BellBlockEntity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.BlockTags
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.LoggerFactory

const val MODID = "village"
val LOGGER = LoggerFactory.getLogger(MODID)

data class BlockAction(val pos: BlockPos, val block: Block)
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
    override fun onInitialize() {
        VillageItems.register()
        FabricDefaultAttributeRegistry.register(VILLAGER, CustomVillagerEntity.createCustomVillagerAttributes())
        // EVENTS
        VillageInteractionCallback.EVENT.register({ player, pos ->
            val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(player.world.getServer()!!)
            val villages = states.villages
            val itemStack: ItemStack = player.getMainHandStack()
            if (itemStack.isOf(Items.NAME_TAG) && itemStack.hasCustomName()) {
                val name = itemStack.getName().getString()
                for (village in villages) {
                    if (village.data.pos == pos) {
                        player.sendMessage(Text.translatable("block.village.bell.interaction.full"), true)
                        return@register ActionResult.FAIL
                    }
                    if (village.data.name == name) {
                        player.sendMessage(Text.translatable("block.village.bell.interaction.same"), true)
                        return@register ActionResult.FAIL
                    }
                    if (village.data.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        player.sendMessage(Text.translatable("block.village.bell.interaction.near"), true)
                        return@register ActionResult.FAIL
                    }
                }
                val k = VillageUtils.getAvailableKey(villages.map { it.data.id })
                villages.add(
                    VillageData(
                        true,
                        Data(k, itemStack.getName().getString(), pos),
                        mutableListOf(),
                        mutableListOf(),
                    ),
                )
                player.sendMessage(Text.translatable("block.village.bell.interaction.new").append(Text.translatable(name)))
            }
            return@register ActionResult.PASS
        })

        VillagerSpawnedCallback.EVENT.register({ entity ->
            val world = entity.world.getServer()
            if (world != null && entity.world is ServerWorld) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(world)
                states.villages.filter { it.isLoaded }.forEach { village ->
                    VillageUtils.addVillager(village, entity)
                    return@register ActionResult.SUCCESS
                }
            }
            return@register ActionResult.FAIL
        })

        VillagerKilledCallback.EVENT.register({ entity ->
            if (!(entity.key == -1)) {
                if (entity.world.getServer() != null && entity.world is ServerWorld) {
                    val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(entity.world.getServer()!!)
                    val village = states.villages.find { it.data.id == entity.villageKey }
                    if (village != null) {
                        VillageUtils.removeVillager(village, entity.key)
                    }
                }
            }
            return@register ActionResult.SUCCESS
        })

        // is sad? to go tavern, is in debpt? go to market! is tired, go home!
        VillagerRequestCallback.EVENT.register({ entity, type ->
            if (entity.world.getServer() != null && entity.world is ServerWorld) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(entity.world.getServer()!!)
                val village = states.villages.find { it.data.id == entity.villageKey }
                if (village != null) {
                    val villager = village.villagers.find { it.first == entity.key }?.second
                    if (villager != null) {
                        val structures = village.structures.filter { it.second.type == type }
                        LOGGER.info("FILTERED STRUCTURES: {}", structures)
                        var structure = structures.find { (it.second.capacity - it.second.owners!!.size) > 0 }
                        if (structure == null) {
                            LOGGER.info("ME HERE!")
                            structure = village.structures.find { it.second.owners!!.contains(entity.key) }
                        }
                        if (structure != null) {
                            // NEED IDEIAS OF STRUCTURES WHERE THE VILLAGER MOVES TO, FROM HOME TO WORK, FROM WORK TO HOME, BUT LIFE IS MORE THAN THAT!
                            LOGGER.info("A STRUCTURE WAS FOUND!")
                            if (!structure.second.owners!!.contains(entity.key)) {
                                structure.second.owners!!.add(entity.key)
                            }
                            entity.assignStructure(structure.first, structure.second)
                        } else {
                            LOGGER.info("NO STRUCTURE FOUND!")
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })
        HandBellUsageCallback.EVENT.register({ player, pos, world ->
            val minecraftServer = world.getServer()
            if (minecraftServer != null) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(minecraftServer)
                val overworld = minecraftServer.getWorld(World.OVERWORLD)
                for (village in states.villages) {
                    if (village.isLoaded && village.data.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        // if (!VillageUtils.isStructureRegion(village.structures, pos)) {
                        //    LOGGER.info("I AM NOT IN ANY STRUCTURE")
                        // }
                        val block = world.getBlockState(pos).getBlock()
                        // TODO: CREATE  THE StructureFactory
                        LOGGER.info("Found block: {}", block.name)
                        when (block) {
                            is FarmlandBlock -> {
                                val farms = village.structures.filter { it.second is Farm }
                                for (farm in farms) {
                                    if ((farm.second.area as Region).center().getManhattanDistance(pos) < 32.0f) {
                                        LOGGER.info("Farm Center: {} Distance", (farm.second.area as Region).center().getManhattanDistance(pos))
                                        player.sendMessage(Text.translatable("item.village.hand_bell.farm.occupied"), true)
                                        // LOGGER.info("THERE IS A FARM HERE ALREADY")
                                        return@register ActionResult.FAIL
                                    }
                                }
                                val farm = Farm.createStructure(pos, overworld as World)
                                if (farm != null) {
                                    val k = VillageUtils.getAvailableKey(village.structures.map { it.first })
                                    village.structures.add(Pair(k, farm))
                                }
                                // LOGGER.info("A FARM WAS ADDED")
                                player.sendMessage(Text.translatable("item.village.hand_bell.farm.added"), true)
                                return@register ActionResult.SUCCESS
                            }
                            is BarrelBlock -> {
                                val ponds = village.structures.filter { it.second is Pond }
                                for (pond in ponds) {
                                    if ((pond.second.area as Point).point.getManhattanDistance(pos) < 32.0f) {
                                        LOGGER.info("Pond Center: {} Distance", (pond.second.area as Point).point.getManhattanDistance(pos))
                                        player.sendMessage(Text.translatable("item.village.hand_bell.pond.occupied"), true)
                                        // LOGGER.info("THERE IS A POND HERE ALREADY")
                                        return@register ActionResult.FAIL
                                    }
                                }
                                val pond = Pond.createStructure(pos, overworld as World)
                                if (pond != null) {
                                    val k = VillageUtils.getAvailableKey(village.structures.map { it.first })
                                    village.structures.add(Pair(k, pond))
                                    player.sendMessage(Text.translatable("item.village.hand_bell.pond.added"), true)
                                }
                                return@register ActionResult.SUCCESS
                            }
                            is DoorBlock -> {
                                // TODO: pass player pos to use in door orientation check
                                //var dpos: BlockPos? = null
                                val door = world.getBlockState(pos)
                                if (door.isIn(BlockTags.DOORS)) {
                                    val direction = door.get(DoorBlock.FACING)
                                    val r = player.blockPos.getSquaredDistance(pos.offset(direction,1).toCenterPos()) 
                                    val l = player.blockPos.getSquaredDistance(pos.offset(direction.getOpposite(),1).toCenterPos()) 
                                    val home = Home.createStructure(if (r > l) {pos.offset(direction,1)} else {pos.offset(direction.getOpposite())}, world)
                                    if (home != null) {
                                        val k = VillageUtils.getAvailableKey(village.structures.map { it.first })
                                        village.structures.add(Pair(k, home))
                                        LOGGER.info("A HOUSE WAS ADDED")
                                        return@register ActionResult.SUCCESS
                                    }
                                    return@register ActionResult.FAIL

                                    
                                    //for (offsetY in 1..16) {
                                    //    if (!world.getBlockState(pos.up(offsetY)).isOf(Blocks.AIR)) {
                                    //        if (!world.getBlockState(pos.up(offsetY).offset(direction, 3)).isOf(Blocks.AIR)) {
                                    //            dpos = pos.offset(direction, 1)
                                    //        }
                                    //        if (!world.getBlockState(pos.up(offsetY).offset(direction.getOpposite(), 3)).isOf(Blocks.AIR)) {
                                    //            dpos = pos.offset(direction.getOpposite(), 1)
                                    //        }
                                    //    }
                                    //}
                                }
                                //if (dpos != null) {
                                //    val home = Home.createStructure(pos, world)
                                //    if (home != null) {
                                //        val k = VillageUtils.getAvailableKey(village.structures.map { it.first })
                                //        village.structures.add(Pair(k, home))
                                //        LOGGER.info("A HOUSE WAS ADDED")
                                //        return@register ActionResult.SUCCESS
                                //    }
                                //    return@register ActionResult.FAIL
                                //}
                            }
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })

        ServerEntityEvents.ENTITY_LOAD.register({ entity, serverWorld ->
            if (entity is CustomVillagerEntity) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                val village = states.villages.find { it.data.id == entity.villageKey }
                if (village != null) {
                    val index = village.villagers.indexOfFirst { it.component1() == entity.key }
                    if (index != -1) {
                        village.villagers[index] = Pair(entity.key, entity)
                        LOGGER.info("| Villager({}) from village: {}, and profession: {}", entity.key, village.data.name, entity.getProfession().type)
                    } else {
                        entity.villageKey = -1
                        entity.key = -1
                        LOGGER.info("ORPHANED")
                    }
                }
            }
        })
        // TODO: maybe just check chunks maybe? which is faster?
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                val serverState: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                for (village in serverState.villages) {
                    if (village.data.pos == blockEntity.getPos()) {
                        village.isLoaded = true
                        LOGGER.info("VILLAGE: {} LOADED", village.data.name)
                    }
                }
            }
        })
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                val serverState: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                for (village in serverState.villages) {
                    if (village.data.pos == blockEntity.getPos()) {
                        village.isLoaded = false
                        LOGGER.info("VILLAGE: {} LOADED", village.data.name)
                    }
                }
            }
        })
    }
}
