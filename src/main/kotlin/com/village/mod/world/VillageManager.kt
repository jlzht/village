package com.village.mod.world

import com.village.mod.BlockAction
import com.village.mod.LOGGER
import com.village.mod.MODID
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.SlabBlock
import net.minecraft.block.Waterloggable
import net.minecraft.block.enums.BedPart
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LightType
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.Queue

enum class StructureType {
    FIREPLACE,
    FARM,
    POND,
    HOUSE,
}

sealed class Structure() {
    abstract val type: StructureType
    open var owners: MutableList<Int>? = null
    abstract var capacity: Int
    abstract var area: Area
    abstract fun interaction(villager: CustomVillagerEntity)
}

sealed class Area

data class Data(var id: Int, var name: String, var pos: BlockPos)

data class Region(var lower: BlockPos, var upper: BlockPos) : Area() {
    fun expand(block: BlockPos) {
        this.lower = BlockPos(
            if (block.x < lower.x) block.x else lower.x,
            if (block.y < lower.y) block.y else lower.y,
            if (block.z < lower.z) block.z else lower.z,
        )
        this.upper = BlockPos(
            if (block.x > upper.x) block.x else upper.x,
            if (block.y > upper.y) block.y else upper.y,
            if (block.z > upper.z) block.z else upper.z,
        )
    }

    fun grow() {
        this.lower = BlockPos(lower.x - 1, lower.y - 1, lower.z - 1)
        this.upper = BlockPos(upper.x + 1, upper.y + 1, upper.z + 1)
    }
    fun area(): Int {
        return (this.upper.x - this.lower.x + 1) * (this.upper.y - this.lower.y + 1) * (this.upper.z - this.lower.z + 1)
    }
    fun center(): BlockPos {
        val middleX = (lower.x + upper.x) / 2
        val middleY = (lower.y + upper.y) / 2
        val middleZ = (lower.z + upper.z) / 2
        return BlockPos(middleX, middleY, middleZ)
    }
    fun contains(coord: BlockPos): Boolean {
        return coord.x >= lower.x && coord.x <= upper.x &&
            coord.y >= lower.y && coord.y <= upper.y &&
            coord.z >= lower.z && coord.z <= upper.z
    }
}

data class Point(var point: BlockPos) : Area()

class Pond(val block: BlockPos) : Structure() {
    override var type: StructureType = StructureType.POND
    override var owners: MutableList<Int>? = mutableListOf()
    override var capacity: Int = 1
    override var area: Area = Point(block)
    override fun interaction(villager: CustomVillagerEntity) {
        LOGGER.info("INTERACTING WITH POND...")
        val world = villager.world
        val pond = (this.area as Point).point
        LOGGER.info("{}", pond)
        if (world.getBlockState(pond).isOf(Blocks.WATER) && world.getBlockState(pond.up()).isOf(Blocks.AIR)) {
            villager.addTargetBlock(BlockAction(pond, Blocks.WATER))
        } else {
            // TODO: remove structure if check fail
            LOGGER.info("DID NOT FOUND VALID POND")
        }
    }
    companion object {
        fun createStructure(pos: BlockPos, world: World): Structure? {
            val water = BlockPos.findClosest(pos, 10, 5, { fpos ->
                world.getBlockState(fpos).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.up()).isOf(Blocks.AIR) &&
                    world.getBlockState(fpos.north()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.south()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.west()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.east()).isOf(Blocks.WATER) &&
                    world.getBlockState(fpos.down()).isOf(Blocks.WATER)
            })
            if (!water.isPresent) {
                return null
            }
            return Pond(water.get())
        }
    }
}

class Home(val residentsCapacity: Int, val lower: BlockPos, val upper: BlockPos, val beds: List<BlockPos>, val flood: BlockPos) : Structure() {
    override var type: StructureType = StructureType.HOUSE
    public var entrance: BlockPos = flood
    override var owners: MutableList<Int>? = mutableListOf()
    public var bedOwners: List<BlockPos> = beds
    override var capacity: Int = residentsCapacity
    override var area: Area = Region(lower, upper)
    override fun interaction(villager: CustomVillagerEntity) {
        villager.addTargetBlock(
            BlockAction(bedOwners[this.owners!!.indexOf(villager.key)], Blocks.RED_BED),
        )
    }
    companion object {
        fun createStructure(pos: BlockPos, world: World): Structure? {
            val queue: Queue<BlockPos> = ArrayDeque()
            val visited = HashSet<BlockPos>()
            var iter: Int = 0
            var itor: Int = 0
            var neigh: Int = 0
            var seigh: Int = 0
            var countLight: Int = 0
            var beds: MutableList<BlockPos> = mutableListOf()
            var seat: MutableList<BlockPos> = mutableListOf()
            val neighbours = listOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, -1, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
            val initialPos = pos
            val edges = Region(initialPos, initialPos)
            queue.add(initialPos)
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.contains(current)) {
                    itor++
                    if (iter >= 32 || itor >= 512) {
                        LOGGER.info("NOT A SUITABLE PLACE")
                        queue.clear()
                        return null
                    }
                    // TODO: optimize this
                    for (neighbor in neighbours) {
                        val blockr = BlockPos(current.x + neighbor.x, current.y + neighbor.y, current.z + neighbor.z)
                        if (!visited.contains(blockr)) {
                            val bk = world.getBlockState(blockr)
                            if (bk.isOf(Blocks.AIR)) {
                                if (world.getLightLevel(LightType.BLOCK, blockr) >= 9) {
                                    countLight++
                                }
                                queue.add(blockr)
                                seigh++
                            } else if (bk.getBlock() is HorizontalFacingBlock && !bk.isIn(BlockTags.TRAPDOORS) && !bk.isIn(BlockTags.FENCE_GATES)) {
                                countLight++
                                if (bk.getBlock() is BedBlock && bk.get(BedBlock.PART) == BedPart.HEAD && !beds.contains(blockr)) {
                                    beds.add(blockr)
                                }
                                queue.add(blockr)
                                seigh++
                            } else if (bk.getBlock() is Waterloggable && visited.contains(blockr.up())) {
                                countLight++
                                if (bk.getBlock() is SlabBlock && !seat.contains(blockr)) {
                                    seat.add(blockr)
                                }
                                queue.add(blockr)
                                seigh++
                            }
                        } else {
                            neigh++
                        }
                    }
                    if ((seigh == 0 && neigh == 3) || (neigh == 1 && seigh == 2) || (neigh == 2 && seigh == 1)) {
                        iter++
                        edges.expand(current)
                    }
                    neigh = 0
                    seigh = 0
                }
                visited.add(current)
            }
            queue.clear()
            if (edges.area() <= countLight) {
                LOGGER.info("SUITABLE LIGHT")
            } else {
                return null
            }
            edges.grow()
            val area = edges.area()
            LOGGER.info("AREA:{}, BEDS:{}", area, beds.size)
            if (beds.size > 0) {
                if (0 < area - (beds.size * 125)) {
                    LOGGER.info("THIS IS SUITABLE")
                } else {
                    LOGGER.info("HOUSE IS TO SMALL FOR CAPACITY")
                    return null
                }
            } else {
                return null
            }
            // LOGGER.info("| area:{} - lightPerBlock:{}: - BedsFound:{} SeatsFound:{}", area, countLight, beds.size, seat.size)
            return Home(beds.size, edges.lower, edges.upper, beds, pos)
        }
    }
}

class Farm(val lower: BlockPos, val upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.FARM
    override var owners: MutableList<Int>? = mutableListOf()
    override var capacity: Int = 1
    override var area: Area = Region(lower, upper)

    private fun getFarmAction(world: World, blockPos: BlockPos, blockState: BlockState): BlockAction? {
        if (blockState.isOf(Blocks.FARMLAND) && blockState.get(FarmlandBlock.MOISTURE) >= 4 && world.getBlockState(blockPos.up()).isOf(Blocks.AIR)) {
            return BlockAction(blockPos.up(), Blocks.WHEAT)
        }
        if (blockState.isOf(Blocks.GRASS_BLOCK) || blockState.isOf(Blocks.DIRT)) {
            if (world.getBlockState(blockPos.up()).isOf(Blocks.AIR)) {
                val north = world.getBlockState(blockPos.north())
                val south = world.getBlockState(blockPos.south())
                val east = world.getBlockState(blockPos.east())
                val west = world.getBlockState(blockPos.west())
                if (
                    (
                        (north.isOf(Blocks.FARMLAND) && north.get(FarmlandBlock.MOISTURE) >= 5) ||
                            (south.isOf(Blocks.FARMLAND) && south.get(FarmlandBlock.MOISTURE) >= 5)
                        ) &&
                    (
                        (west.isOf(Blocks.FARMLAND) && west.get(FarmlandBlock.MOISTURE) >= 5) ||
                            (east.isOf(Blocks.FARMLAND) && east.get(FarmlandBlock.MOISTURE) >= 5)
                        )
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
                        return BlockAction(blockPos, Blocks.FARMLAND)
                    }
                }
            }
        }
        return null
    }

    override fun interaction(villager: CustomVillagerEntity) {
        LOGGER.info("INTERACTING WITH FARM...")
        val world = villager.world
        val region = (this.area as Region)
        val farmCenter = region.center()
        // SQUARE ITERATION?
        for (x in (region.lower.x - 1)..(region.upper.x + 1)) {
            for (z in (region.lower.z - 1)..(region.upper.z + 1)) {
                val farmPos = BlockPos(x, farmCenter.y, z)
                if (farmCenter.getManhattanDistance(farmPos) < 12.0f) {
                    val blockAction = getFarmAction(world, farmPos, world.getBlockState(farmPos))
                    if (blockAction != null) {
                        villager.addTargetBlock(blockAction)
                        if (!region.contains(farmPos)) {
                            region.expand(farmPos)
                        }
                    }
                }
            }
        }
    }
    companion object {
        fun createStructure(pos: BlockPos, world: World): Structure? {
            if (
                world.getBlockState(pos.north().west()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.north().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().east()).isOf(Blocks.FARMLAND) ||
                world.getBlockState(pos.south().west()).isOf(Blocks.FARMLAND)
            ) {
                return Farm(pos, pos)
            } else {
                return null
            }
        }
    }
}

data class VillageData(
    var isLoaded: Boolean,
    var data: Data,
    var structures: MutableList<Pair<Int, Structure>>,
    var villagers: MutableList<Pair<Int, CustomVillagerEntity?>>,
)

class StateSaverAndLoader : PersistentState() {
    var cooldown: Int = 0
    var villages: MutableList<VillageData> = mutableListOf()
    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", villagesSerialize())
        return nbt
    }

    protected fun villagesSerialize(): NbtList {
        val nbtList = NbtList()
        for (village in villages) {
            val villageData: NbtCompound = NbtCompound()
            villageData.putInt("VillageKey", village.data.id)
            villageData.putString("VillageName", village.data.name)
            villageData.putInt("VillageCenterPosX", village.data.pos.x)
            villageData.putInt("VillageCenterPosY", village.data.pos.y)
            villageData.putInt("VillageCenterPosZ", village.data.pos.z)
            villageData.putIntArray("VillagersData", village.villagers.map { it.component1() })
            LOGGER.info("{}", village.structures)
            villageData.put("StructuresData", structureSerialize(village.structures))
            nbtList.add(villageData)
        }
        return nbtList
    }
    protected fun structureSerialize(structures: MutableList<Pair<Int, Structure>>): NbtList {
        val nbtList = NbtList()
        LOGGER.info("GOT IN STRUCTURE")
        for (structure in structures) {
            LOGGER.info("ADDED STRUCTURE")
            val structureData: NbtCompound = NbtCompound()
            structureData.putInt("StructureKey", structure.first)
            structureData.putInt("StructureType", structure.second.type.ordinal)
            structureData.putIntArray("StructureOwners", structure.second.owners)
            if (structure.second.type == StructureType.HOUSE) {
                structureData.putInt("StructureEntranceX", (structure.second as Home).entrance.x)
                structureData.putInt("StructureEntranceY", (structure.second as Home).entrance.y)
                structureData.putInt("StructureEntranceZ", (structure.second as Home).entrance.z)
            }
            structureData.putInt("StructureCapacity", structure.second.capacity)
            if (structure.second.area is Region) {
                // Use only lower block and store work(blocks needed to arrive at upper)  -> two less intagers to store
                structureData.putInt("StructureAreaLowerX", (structure.second.area as Region).lower.x)
                structureData.putInt("StructureAreaLowerY", (structure.second.area as Region).lower.y)
                structureData.putInt("StructureAreaLowerZ", (structure.second.area as Region).lower.z)
                structureData.putInt("StructureAreaUpperX", (structure.second.area as Region).upper.x)
                structureData.putInt("StructureAreaUpperY", (structure.second.area as Region).upper.y)
                structureData.putInt("StructureAreaUpperZ", (structure.second.area as Region).upper.z)
            } else {
                structureData.putInt("StructureAreaPointX", (structure.second.area as Point).point.x)
                structureData.putInt("StructureAreaPointY", (structure.second.area as Point).point.y)
                structureData.putInt("StructureAreaPointZ", (structure.second.area as Point).point.z)
            }
            nbtList.add(structureData)
        }
        return nbtList
    }

    companion object {
        fun createFromNbt(tag: NbtCompound): StateSaverAndLoader {
            val state = StateSaverAndLoader()
            val villagesNbt = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0..villagesNbt.count() - 1) {
                val data = villagesNbt.getCompound(i)
                val villagers = data.getIntArray("VillagersData").toList()
                val structures = data.getList("StructuresData", NbtElement.COMPOUND_TYPE.toInt())
                val structureList: MutableList<Pair<Int, Structure>> = mutableListOf()
                for (l in 0..structures.count() - 1) {
                    val sdata = structures.getCompound(l)
                    val ltrs: Structure? = when (sdata.getInt("StructureType")) {
                        StructureType.HOUSE.ordinal -> Home(
                            sdata.getInt("StructureCapacity"),
                            BlockPos(sdata.getInt("StructureAreaLowerX"), sdata.getInt("StructureAreaLowerY"), sdata.getInt("StructureAreaLowerZ")),
                            BlockPos(sdata.getInt("StructureAreaUpperX"), sdata.getInt("StructureAreaUpperY"), sdata.getInt("StructureAreaUpperZ")),
                            mutableListOf(), // TODO: find a good implementation to handle bed positions
                            BlockPos(sdata.getInt("StructureEntranceX"), sdata.getInt("StructureEntranceY"), sdata.getInt("StructureEntranceZ")),
                        )
                        StructureType.POND.ordinal -> Pond(
                            BlockPos(sdata.getInt("StructureAreaPointX"), sdata.getInt("StructureAreaPointY"), sdata.getInt("StructureAreaPointZ")),
                        )
                        StructureType.FARM.ordinal -> Farm(
                            BlockPos(sdata.getInt("StructureAreaLowerX"), sdata.getInt("StructureAreaLowerY"), sdata.getInt("StructureAreaLowerZ")),
                            BlockPos(sdata.getInt("StructureAreaUpperX"), sdata.getInt("StructureAreaUpperY"), sdata.getInt("StructureAreaUpperZ")),
                        )
                        else -> null
                    }
                    LOGGER.info("GOT HERE!")
                    if (ltrs != null) {
                        ltrs.owners = sdata.getIntArray("StructureOwners").toMutableList()
                        ltrs.capacity = sdata.getInt("StructureCapacity")
                        structureList.add(Pair(sdata.getInt("StructureKey"), ltrs))
                    }
                }
                state.villages.add(
                    VillageData(
                        false,
                        Data(
                            data.getInt("VillageKey"),
                            data.getString("VillageName"),
                            BlockPos(data.getInt("VillageCenterPosX"), data.getInt("VillageCenterPosY"), data.getInt("VillageCenterPosZ")),
                        ),
                        structureList,
                        villagers.map { it to null }.toMutableList(),
                    ),
                )
            }
            return state
        }

        private val type = Type({ StateSaverAndLoader() }, ::createFromNbt, null)

        fun getServerState(server: MinecraftServer): StateSaverAndLoader {
            val persistentStateManager = server.getWorld(World.OVERWORLD)?.persistentStateManager
                ?: throw IllegalStateException("Failed to get PersistentStateManager for OVERWORLD")

            val state = persistentStateManager.getOrCreate(type, MODID)
            state.markDirty()
            return state
        }
    }
}

object VillageUtils {
    fun getAvailableKey(existingNumbers: List<Int>): Int {
        var idNumber: Int = 0
        do {
            idNumber = ++idNumber
        } while (idNumber in existingNumbers)
        return idNumber
    }

    fun addVillager(village: VillageData, entity: CustomVillagerEntity) {
        val k = VillageUtils.getAvailableKey(village.villagers.map { it.first })
        village.villagers.add(Pair(k, entity))
        entity.key = k
        entity.villageKey = village.data.id
        // entity.setState(State.TRAVEL) // will autoset when pushing
        entity.addTargetBlock(BlockAction(village.data.pos, Blocks.BELL))
    }
    fun removeVillager(village: VillageData, id: Int) {
        village.villagers.removeIf { it.first == id }
        village.structures.removeIf { it.second.owners!!.contains(id) }
    }

    // fun isStructureRegion(structures: List<Structure>, pos: BlockPos): Boolean {
    //    for (structure in structures) {
    //        if (structure.area is Region) {
    //            if ((structure.area as Region).contains(pos)) {
    //                return true
    //            }
    //        }
    //    }
    //    return false
    // }
}
