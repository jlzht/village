package com.village.mod.world

import com.village.mod.LOGGER
import com.village.mod.MODID
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.entity.village.MutablePair
import com.village.mod.village.structure.Building
import com.village.mod.village.structure.Farm
import com.village.mod.village.structure.Point
import com.village.mod.village.structure.Pond
import com.village.mod.village.structure.Region
import com.village.mod.village.structure.Area
import com.village.mod.village.structure.Structure
import com.village.mod.village.structure.StructureFactory
import com.village.mod.village.structure.StructureType
import com.village.mod.village.villager.Action
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World



class Village(var isLoaded: Boolean, val id: Int, val name: String, val pos: BlockPos) {
    public var structures: HashMap<Int, Structure> = hashMapOf()
    public var villagers: MutableMap<Int, CustomVillagerEntity?> = mutableMapOf()
    constructor(
        isLoaded: Boolean,
        id: Int,
        name: String,
        pos: BlockPos,
        structures: HashMap<Int, Structure>,
        villagers: MutableMap<Int, CustomVillagerEntity?>,
    ) : this(isLoaded, id, name, pos) {
        this.structures = structures
        this.villagers = villagers
    }

    public fun createStructure(pos: BlockPos) {
    }

    public fun addStructure(structure: Structure) {
        val key = Village.getAvailableKey(this.structures.map { it.key })
        this.structures[key] = structure
    }

    public fun removeStructure(id: Int) {
        this.structures.remove(id)
    }

    public fun addVillager(entity: CustomVillagerEntity) {
        val key = Village.getAvailableKey(this.villagers.map { it.key })
        this.villagers[key] = entity
        entity.key = key
        entity.villageKey = this.id
        entity.errand.push(Errand(this.pos, Action.MOVE))
    }

    public fun removeVillager(id: Int) {
        this.villagers.remove(id)
        this.structures.filter { it.value.owners.contains(id) }.forEach { structure ->
            structure.value.owners.remove(id)
        }
    }
    public fun assignStructureToVillager(key: Int, type: StructureType, structureAttacher: MutablePair) {
        if (structureAttacher.key != 0) {
            this.structures.entries.filter { it.value.type == type && it.key == structureAttacher.key }.firstOrNull()?.let { structure ->
                structureAttacher.key = structure.key
                structureAttacher.structure = structure.value
            }
        } else {
            this.structures.entries.filter { it.value.type == type && (it.value.capacity - it.value.owners.size) > 0 }.firstOrNull()?.let { structure ->
                structure.value.owners.add(key)
                structureAttacher.key = structure.key
                structureAttacher.structure = structure.value
            }
        }
    }

    fun isStructureInRegion(pos: BlockPos): Boolean {
      for (structure in structures.values) {
          //if (structure.area is Point) {
          //    if ((structure.area as Point).point == pos) {
          //        return true
          //    }
          //} else {
              if ((structure.area as Region).contains(pos)) {
                  return true
              }
          //}
      }
      return false
    }
    fun isStructureIsRange(range: Float): Boolean {
        for (structure in structures.values) {
          //if (structure.area is Point) {
          //  if ((structure.area as Point).point.getManhattanDistance(pos) < range) {
          //      return false
          //  }
          //} else {
            if ((structure.area as Region).center().getManhattanDistance(pos) < range) {
                return false
            }
          //}
        }
        return true
    }

    companion object {
        fun getAvailableKey(existingNumbers: List<Int>): Int {
            var idNumber: Int = 0
            do {
                idNumber = ++idNumber
            } while (idNumber in existingNumbers)
            return idNumber
        }
    }
}

class VillageSaverAndLoader : PersistentState() {
    // TODO: use HashMap inside of a Manager class instead of MutableList
    var villages: MutableList<Village> = mutableListOf()
    fun addVillage(name: String, pos: BlockPos) {
        val k = Village.getAvailableKey(villages.map { it.id })
        villages.add(Village(false, k, name, pos))
    }
    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", villagesSerialize())
        return nbt
    }

    protected fun villagesSerialize(): NbtList {
        val nbtList = NbtList()
        for (village in villages) {
            val villageData: NbtCompound = NbtCompound()
            villageData.putInt("VillageKey", village.id)
            villageData.putString("VillageName", village.name)
            villageData.putInt("VillageCenterPosX", village.pos.x)
            villageData.putInt("VillageCenterPosY", village.pos.y)
            villageData.putInt("VillageCenterPosZ", village.pos.z)
            villageData.putIntArray("VillagersData", village.villagers.map { it.component1() })
            LOGGER.info("{}", village.structures)
            villageData.put("StructuresData", structureSerialize(village.structures))
            nbtList.add(villageData)
        }
        return nbtList
    }
    protected fun structureSerialize(structures: HashMap<Int, Structure>): NbtList {
        LOGGER.info("GOT IN STRUCTURE")
        val nbtList = NbtList()
        for (structure in structures) {
            val structureData: NbtCompound = NbtCompound()
            structureData.putInt("StructureKey", structure.key)
            structureData.putInt("StructureType", structure.value.type.ordinal)
            structureData.putIntArray("StructureOwners", structure.value.owners)
            structureData.putInt("StructureCapacity", structure.value.capacity)
            if (structure.value is Building) {
                structureData.putInt("StructureEntranceX", (structure.value as Building).entrance.x)
                structureData.putInt("StructureEntranceY", (structure.value as Building).entrance.y)
                structureData.putInt("StructureEntranceZ", (structure.value as Building).entrance.z)
            }
            if (structure.value.area is Region) {
                // Use only lower block and store work(blocks needed to arrive at upper)  -> two less intagers to store
                structureData.putInt("StructureAreaLowerX", (structure.value.area as Region).lower.x)
                structureData.putInt("StructureAreaLowerY", (structure.value.area as Region).lower.y)
                structureData.putInt("StructureAreaLowerZ", (structure.value.area as Region).lower.z)
                structureData.putInt("StructureAreaUpperX", (structure.value.area as Region).upper.x)
                structureData.putInt("StructureAreaUpperY", (structure.value.area as Region).upper.y)
                structureData.putInt("StructureAreaUpperZ", (structure.value.area as Region).upper.z)
            } else {
                structureData.putInt("StructureAreaPointX", (structure.value.area as Point).point.x)
                structureData.putInt("StructureAreaPointY", (structure.value.area as Point).point.y)
                structureData.putInt("StructureAreaPointZ", (structure.value.area as Point).point.z)
            }
            nbtList.add(structureData)
        }
        return nbtList
    }

    companion object {
        fun createFromNbt(tag: NbtCompound): VillageSaverAndLoader {
            val state = VillageSaverAndLoader()
            val villagesNbt = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0..villagesNbt.count() - 1) {
                val data = villagesNbt.getCompound(i)
                val villagers = data.getIntArray("VillagersData").toList()
                val structures = data.getList("StructuresData", NbtElement.COMPOUND_TYPE.toInt())
                val structureList: HashMap<Int, Structure> = hashMapOf()
                for (l in 0..structures.count() - 1) {
                    val sdata = structures.getCompound(l)
                    val structure: Structure? = when (sdata.getInt("StructureType")) {
                        in 5..8 -> Building(
                            StructureType.values()[sdata.getInt("StructureType")],
                            BlockPos(sdata.getInt("StructureAreaLowerX"), sdata.getInt("StructureAreaLowerY"), sdata.getInt("StructureAreaLowerZ")),
                            BlockPos(sdata.getInt("StructureAreaUpperX"), sdata.getInt("StructureAreaUpperY"), sdata.getInt("StructureAreaUpperZ")),
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
                    if (structure != null) {
                        structure.owners = sdata.getIntArray("StructureOwners").toMutableList()
                        structure.capacity = sdata.getInt("StructureCapacity")
                        structureList[sdata.getInt("StructureKey")] = structure
                    }
                }
                state.villages.add(
                    Village(
                        false,
                        data.getInt("VillageKey"),
                        data.getString("VillageName"),
                        BlockPos(data.getInt("VillageCenterPosX"), data.getInt("VillageCenterPosY"), data.getInt("VillageCenterPosZ")),
                        structureList,
                        villagers.map { it to null }.toMap().toMutableMap(),
                    ),
                )
            }
            return state
        }

        private val type = Type({ VillageSaverAndLoader() }, ::createFromNbt, null)

        fun getServerState(server: MinecraftServer): VillageSaverAndLoader {
            val persistentStateManager = server.getWorld(World.OVERWORLD)?.persistentStateManager
                ?: throw IllegalStateException("Failed to get PersistentStateManager for OVERWORLD")

            val state = persistentStateManager.getOrCreate(type, MODID)
            state.markDirty()
            return state
        }
    }
}
