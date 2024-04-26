package com.village.mod

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.item.VillageItems
import com.village.mod.screen.TradingScreenHandler
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.villager.State
import com.village.mod.world.StateSaverAndLoader
import com.village.mod.world.StructureData
import com.village.mod.world.StructureType
import com.village.mod.world.VillageData
import com.village.mod.world.event.HandBellUsageCallback
import com.village.mod.world.event.VillageInteractionCallback
import com.village.mod.world.event.VillagerRequestCallback
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BellBlockEntity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.BlockItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import net.minecraft.block.FarmlandBlock

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
    override fun onInitialize() {
        VillageItems.register()
        FabricDefaultAttributeRegistry.register(VILLAGER, CustomVillagerEntity.createCustomVillagerAttributes())

        VillageInteractionCallback.EVENT.register({ player, pos ->
            val serverState: StateSaverAndLoader = StateSaverAndLoader.getServerState(player.world.getServer()!!)
            val itemStack: ItemStack = player.getMainHandStack()
            if (itemStack.isOf(Items.NAME_TAG) && itemStack.hasCustomName()) {
                val name = itemStack.getName().getString()
                var ff: MutableList<Int> = mutableListOf()
                for (village in serverState.villages) {
                    if (village.pos == pos) {
                        LOGGER.info("THIS IS ALREADY A VILLAGE")
                        return@register ActionResult.FAIL
                    }
                    if (village.name == name) {
                        LOGGER.info("ANOTHER VILLAGE HAS THIS NAME")
                        return@register ActionResult.FAIL
                    }
                    if (village.pos.getSquaredDistance(pos.toCenterPos()) < 128.0f) {
                        LOGGER.info("THERE IS ANOTHER VILLAGE TOO CLOSE")
                        return@register ActionResult.FAIL
                    }
                    ff.add(village.id)
                }
                val k = generateNewNumber(ff)
                serverState.villages.add(VillageData(k, itemStack.getName().getString(), mutableListOf(), true, pos, mutableListOf(), mutableListOf()))
                LOGGER.info("CREATE VILLAGE NAMED: $name({})", k)
            }
            return@register ActionResult.PASS
        })

        VillagerRequestCallback.EVENT.register({ entity, id ->
            if (entity.world.getServer() != null && entity.world is ServerWorld) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(entity.world.getServer()!!)
                if (id == 0) {
                    states.villages.filter { it.isLoaded }.forEach { village ->
                        LOGGER.info("I SETTED THE VILLAGE TO MOVE")
                        val villagers = village.villagers
                        val ff = villagers.map { it.first }
                        val k = generateNewNumber(ff)
                        villagers.add(Pair(k, entity))
                        entity.identification = k
                        entity.villageIdentification = village.id
                        entity.mark = 3
                        entity.setState(State.TRAVEL)
                        entity.pushTargetBlock(village.pos)
                        return@register ActionResult.SUCCESS
                    }
                } else if (id == 1) {
                    states.villages.find { it.id == entity.villageIdentification }?.villagers?.removeIf { it.first == entity.identification }
                    return@register ActionResult.SUCCESS
                } else if (id == 2) {
                    val village = states.villages.find { it.id == entity.villageIdentification }
                    if (village != null) {
                        val villager = village.villagers.find { it.first == entity.identification }?.second
                        if (villager != null) {
                            // FARMER
                            if (villager.getProfession().type == ProfessionType.FARMER) {
                                LOGGER.info("I AM A FARMER")
                                val farm = village.structures.find { it.component1() == StructureType.FARM && it.component2() == entity.identification }
                                LOGGER.info("{}", farm)
                                // STRUCTURE INTERACTION
                                if (farm != null) {
                                    LOGGER.info("I HAVE A FARM")
                                    val lowerX = farm.component3().x - 1
                                    val lowerZ = farm.component3().z - 1
                                    val upperX = farm.component4().x + 1
                                    val upperZ = farm.component4().z + 1
                                    val farmCenter = findMiddlePoint(farm.component3(), farm.component4())
                                    LOGGER.info("{}", farmCenter)
                                    for (x in lowerX..upperX) {
                                        for (z in lowerZ..upperZ) {
                                            val farmPos = BlockPos(x, farmCenter.y, z)
                                            if (farmCenter.getManhattanDistance(farmPos) < 12.0f) {
                                                LOGGER.info("{} - {}", farmPos, entity.world.getBlockState(farmPos))
                                                val farmldd = isValidFarmland(entity.world, farmPos)
                                                if (farmldd == farmlander.TILL) {
                                                    LOGGER.info("I FOUND VALID FARMLAND")
                                                    entity.pushTargetBlock(farmPos)
                                                    val kds = tryAppendToZOI(farm.component3(), farm.component4(), farmPos)
                                                    farm.lowerBlock = kds.first
                                                    farm.upperBlock = kds.second
                                                    LOGGER.info("LOWER BOUND:{} UPPER BOUND {} FARM CENTER {}", farm.lowerBlock, farm.upperBlock, farmCenter)
                                                } else if (farmldd == farmlander.VALID) {
                                                    if (!isWithinRange(farmPos, farm.component3(), farm.component4())) {
                                                        // DO A ENCLOSING CHECK
                                                        val kds = tryAppendToZOI(farm.component3(), farm.component4(), farmPos)
                                                        farm.lowerBlock = kds.first
                                                        farm.upperBlock = kds.second
                                                        LOGGER.info("LOWER BOUND:{} UPPER BOUND {} FARM CENTER {}", farm.lowerBlock, farm.upperBlock, farmCenter)
                                                    } else {
                                                      LOGGER.info("I AM ELSE!")
                                                      val lda = entity.world.getBlockState(farmPos.up()).isOf(Blocks.AIR)
                                                      if (lda) {
                                                        // TODO: make it be block pushable
                                                        // if has seeds push block action in goto
                                                        val gde = (ItemStack(Items.WHEAT_SEEDS).getItem() as BlockItem).getBlock().getDefaultState()
                                                        entity.world.setBlockState(farmPos.up(), gde)
                                                      }

                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    var availableFarm = village.structures.find { it.component1() == StructureType.FARM && it.component2() == -1 }
                                    if (availableFarm != null) {
                                        LOGGER.info("FARM OWNED ADDED")
                                        availableFarm.owner = entity.identification
                                    }
                                }
                            }
                            // FISHERMAN
                            if (villager.getProfession().type == ProfessionType.FISHERMAN) {
                            }
                        }
                    }

                    // consume block
                }
            }
            return@register ActionResult.PASS
        })

        HandBellUsageCallback.EVENT.register({ pos, world ->
            val minecraftServer = world.getServer()
            if (minecraftServer != null) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(minecraftServer)
                val overworld = minecraftServer.getWorld(World.OVERWORLD)
                for (village in states.villages) {
                    // TODO: Use village radius instead of fixed radius
                    if (village.isLoaded && village.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        looper@ for (x in -8..8) {
                            for (y in -8..8) {
                                for (z in -8..8) {
                                    // CHECK
                                    var apos = pos.add(x, y, z)
                                    if (isValidFarmland(overworld as World, apos) == farmlander.TILL) {
                                        val farms = village.structures.filter { it.component1() == StructureType.FARM }
                                        if (farms.isEmpty()) {
                                            LOGGER.info("I AM NULL")
                                            LOGGER.info("I ADDED FARMLAND!")
                                            village.structures.add(StructureData(StructureType.FARM, -1, apos, apos, 1))
                                            break@looper
                                        } else {
                                            for (farm in farms) {
                                                if (findMiddlePoint(farm.component3(), farm.component4()).getManhattanDistance(apos) < 13.0f) {
                                                    break@looper
                                                }
                                            }
                                            LOGGER.info("I ADDED FARMLAND!")
                                            village.structures.add(StructureData(StructureType.FARM, -1, apos, apos, 1))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return@register ActionResult.PASS
        })

        // ServerTickEvents.END_SERVER_TICK.register({ minecraftServer ->
        // })

        ServerEntityEvents.ENTITY_LOAD.register({ entity, serverWorld ->
            if (entity is CustomVillagerEntity) {
                val states: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                val village = states.villages.find { it.id == entity.villageIdentification }
                if (village != null) {
                    val index = village.villagers.indexOfFirst { it.component1() == entity.identification }
                    if (index != -1) {
                        village.villagers[index] = Pair(entity.identification, entity)
                        LOGGER.info("PARENTED - {} = {}", entity.villageIdentification, entity.identification)
                    } else {
                        entity.villageIdentification = -1
                        entity.identification = -1
                        LOGGER.info("ORPHAN")
                    }
                }
            }
        })
        // TODO: optimize acess
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                val serverState: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                for (village in serverState.villages) {
                    if (village.component5() == blockEntity.getPos()) {
                        village.isLoaded = true
                        for (villager in village.villagers) {
                            // ADD BETTER LOG
                            LOGGER.info("|{}", villager.component1())
                        }
                        LOGGER.info("I LOADED A VILLAGE")
                    }
                }
            }
        })
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register({ blockEntity, serverWorld ->
            if (blockEntity is BellBlockEntity) {
                val serverState: StateSaverAndLoader = StateSaverAndLoader.getServerState(serverWorld.getServer())
                for (village in serverState.villages) {
                    if (village.component5() == blockEntity.getPos()) {
                        village.isLoaded = false
                        LOGGER.info("I UNLOADED A VILLAGE")
                    }
                }
            }
        })
    }
    private fun findMiddlePoint(point1: BlockPos, point2: BlockPos): BlockPos {
        val middleX = (point1.x + point2.x) / 2
        val middleY = (point1.y + point2.y) / 2
        val middleZ = (point1.z + point2.z) / 2
        return BlockPos(middleX, middleY, middleZ)
    }
    public fun isWithinRange(coord: BlockPos, lowerBound: BlockPos, upperBound: BlockPos): Boolean {
        return coord.getX() >= lowerBound.getX() && coord.getX() <= upperBound.getX() &&
               coord.getY() >= lowerBound.getY() && coord.getY() <= upperBound.getY() &&
               coord.getZ() >= lowerBound.getZ() && coord.getZ() <= upperBound.getZ();
    }
    // TODO: Optimize this
    public fun tryAppendToZOI(lowerBound: BlockPos, upperBound: BlockPos, blockPos: BlockPos): Pair<BlockPos, BlockPos> {
        var newUpperBound: BlockPos = upperBound
        var newLowerBound: BlockPos = lowerBound
        if (lowerBound.equals(upperBound)) {
            newUpperBound = blockPos
        }
        if (lowerBound.x == 0 && lowerBound.z == 0) {
            newLowerBound = upperBound
        }
        if (blockPos.x > upperBound.x) {
            newUpperBound = upperBound.add(blockPos.x - upperBound.x, 0, 0)
        }
        if (blockPos.z > upperBound.z) {
            newUpperBound = upperBound.add(0, 0, blockPos.z - upperBound.z)
        }
        if (blockPos.x < lowerBound.x) {
            newLowerBound = lowerBound.add(blockPos.x - lowerBound.x, 0, 0)
        }
        if (blockPos.z < lowerBound.z) {
            newLowerBound = lowerBound.add(0, 0, blockPos.z - lowerBound.z)
        }
        return Pair(newLowerBound, newUpperBound)
    }
    enum class farmlander {
      NONE,
      VALID,
      TILL
    }
    // TODO: Create a tag group for farmable blocks
    private fun isValidFarmland(world: World, blockPos: BlockPos): farmlander {
        val block = world.getBlockState(blockPos)
        if (block.isOf(Blocks.FARMLAND)) {
          return farmlander.VALID
        }
        if (block.isOf(Blocks.GRASS_BLOCK) || block.isOf(Blocks.DIRT)) {
            if (world.getBlockState(blockPos.up()).isOf(Blocks.AIR)) {
                val north = world.getBlockState(blockPos.north())
                val south = world.getBlockState(blockPos.south())
                val east  = world.getBlockState(blockPos.east())
                val west  = world.getBlockState(blockPos.west())
                if (
                    ((north.isOf(Blocks.FARMLAND) && north.get(FarmlandBlock.MOISTURE) == 7) || (south.isOf(Blocks.FARMLAND) && south.get(FarmlandBlock.MOISTURE) == 7)) &&
                    ((west.isOf(Blocks.FARMLAND) && west.get(FarmlandBlock.MOISTURE) == 7) || (east.isOf(Blocks.FARMLAND) && east.get(FarmlandBlock.MOISTURE) == 7))
                ) {
                    val northwest = world.getBlockState(blockPos.north().west())
                    val northeast = world.getBlockState(blockPos.north().east())                    
                    val southeast = world.getBlockState(blockPos.south().east())
                    val southwest = world.getBlockState(blockPos.south().west())
                    if (
                        (northeast.isOf(Blocks.FARMLAND) || northeast.isOf(Blocks.WATER)) ||
                        (northwest.isOf(Blocks.FARMLAND) || northwest.isOf(Blocks.WATER)) ||
                        (southeast.isOf(Blocks.FARMLAND) || southeast.isOf(Blocks.WATER)) ||
                        (southwest.isOf(Blocks.FARMLAND) || southwest.isOf(Blocks.WATER))
                    ) {
                        return farmlander.TILL
                    }
                }
            }
        }
        return farmlander.NONE
    }

    fun generateNewNumber(existingNumbers: List<Int>): Int {
        var idNumber: Int = 0
        do {
            idNumber = ++idNumber
        } while (idNumber in existingNumbers)
        return idNumber
    }
}
