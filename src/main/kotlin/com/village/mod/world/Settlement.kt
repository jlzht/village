package com.village.mod.world

import com.village.mod.action.Errand
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.network.SettlementDebugData
import com.village.mod.screen.Response
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.structure.Building
import com.village.mod.village.structure.Farm
import com.village.mod.village.structure.Pond
import com.village.mod.village.structure.Structure
import com.village.mod.village.structure.StructureType
import net.minecraft.block.BarrelBlock
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import java.util.UUID
import kotlin.collections.toPair

data class Settler(
    val id: Int,
    var villager: CustomVillagerEntity?,
    val errand: MutableList<Errand> = mutableListOf(),
)

data class PlayerDataField(
    val reputation: Int,
    val uuid: UUID,
)

// TODO:
// - Decide if having a villager reference to access direcly is 'good' or 'bad'
// - make a public Errand field
// - remove isLoaded variable
// - implement leveling system
// - replace PlayerDataField with Map<UUID, Int>
// - make pos the median center point (relative to structure centers)
class Settlement(
    val id: Int,
    val name: String,
    val pos: BlockPos,
    val dim: String, // use less data to represent state
) {
    var structures = mutableMapOf<Int, Structure>()
    var settlers = mutableListOf<Settler>()
    var allies = arrayListOf<PlayerDataField>()
    var level: Int = 1

    constructor(
        id: Int,
        name: String,
        pos: BlockPos,
        dim: String,
        structures: MutableMap<Int, Structure>,
        settlers: MutableList<Settler>,
        allies: ArrayList<PlayerDataField>,
    ) : this(id, name, pos, dim) {
        this.structures = structures
        this.settlers = settlers
        this.allies = allies
    }

    fun createStructure(
        pos: BlockPos,
        player: PlayerEntity,
    ) {
        when (player.world.getBlockState(pos).getBlock()) {
            is FarmlandBlock -> {
                if (
                    player.world.getBlockState(pos.north().west()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.north().east()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.south().east()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.south().west()).isOf(Blocks.FARMLAND)
                ) {
                    Farm.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
                }
            }
            is BarrelBlock -> { // Find a way to interact with water
                if (!this.isStructureInRange(pos, 8.0f)) {
                    Pond.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
                }
            }
            is DoorBlock -> {
                if (!this.isStructureInRegion(pos)) {
                    Building.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_INSIDE.send(player).run { null }
                }
            }
            else -> Response.INVALID_BLOCK.send(player).run { null }
        }?.let {
            this.addStructure(it)
        }
    }

    fun addStructure(structure: Structure) {
        // TODO: create method that finds allies players by UUID and sendMessage to them notifying structure creating
        val key = Settlement.getAvailableKey(this.structures.map { it.key })
        this.structures[key] = structure
    }

    fun getStructure(id: Int): Structure? = structures[id]

    fun getStructureByType(type: StructureType): Pair<Int, Structure>? =
        this.structures
            .filter {
                it.value.type == type && it.value.isAvailable()
            }.entries
            .firstOrNull()
            ?.toPair()

    fun removeStructure(id: Int) {
        this.structures.remove(id)
    }

    fun addVillager(entity: CustomVillagerEntity) {
        val key = Settlement.getAvailableKey(this.settlers.map { it.id })
        this.settlers.add(Settler(key, entity))
        entity.data.key = key
        entity.data.sid = this.id
    }

    fun removeVillager(id: Int) {
        this.settlers.removeIf { it.id == id }
    }

    fun getStructureInRegion(pos: BlockPos): StructureType {
        for (structure in structures.values) {
            if (structure.region.contains(pos)) {
                return structure.type
            }
        }
        return StructureType.NONE
    }

    fun isStructureInRegion(pos: BlockPos): Boolean {
        for (structure in structures.values) {
            if (structure.region.contains(pos)) {
                return true
            }
        }
        return false
    }

    fun isStructureInRange(
        pos: BlockPos,
        range: Float,
    ): Boolean {
        for (structure in structures.values) {
            if (pos.getManhattanDistance(structure.region.center()) < range) {
                return true
            }
        }
        return false
    }

    fun getProfessionsBySettlementLevel(): List<ProfessionType> {
        val levelProfessionMap =
            mapOf(
                0 to listOf(ProfessionType.GATHERER, ProfessionType.HUNTER),
                1 to listOf(ProfessionType.GUARD, ProfessionType.FARMER, ProfessionType.FISHERMAN),
            )
        val professions = levelProfessionMap[level] ?: listOf(ProfessionType.NONE)
        return professions
    }

    fun toNbt(): NbtCompound =
        NbtCompound().apply {
            putInt("VillageKey", id)
            putString("VillageName", name)
            putInt("SettlementOriginPosX", pos.x)
            putInt("SettlementOriginPosY", pos.y)
            putInt("SettlementOriginPosZ", pos.z)
            putString("DimensionType", dim)
            putIntArray("VillagersData", settlers.map { it.id })
            put("StructuresData", structuresSerialize())
            put("AlliesData", alliesSerialize())
        }

    fun alliesSerialize(): NbtList {
        val nbtList = NbtList()
        for (ally in allies) {
            val allyData = NbtCompound()
            allyData.putUuid("AllyUUID", ally.uuid)
            allyData.putInt("AllyReputation", ally.reputation)
            nbtList.add(allyData)
        }
        return nbtList
    }

    fun structuresSerialize(): NbtList {
        val nbtList = NbtList()
        for (structure in structures) {
            val structureData = NbtCompound()
            structureData.putInt("StructureKey", structure.key)
            structureData.putInt("StructureType", structure.value.type.ordinal)
            structureData.putInt("StructureCapacity", structure.value.capacity)
            structureData.putIntArray("StructureSettlers", structure.value.getResidents())
            structureData.putInt("StructureAreaLowerX", structure.value.region.lower.x) // convert blockPos to long
            structureData.putInt("StructureAreaLowerY", structure.value.region.lower.y)
            structureData.putInt("StructureAreaLowerZ", structure.value.region.lower.z)
            structureData.putInt("StructureAreaUpperX", structure.value.region.upper.x)
            structureData.putInt("StructureAreaUpperY", structure.value.region.upper.y)
            structureData.putInt("StructureAreaUpperZ", structure.value.region.upper.z)
            nbtList.add(structureData)
        }
        return nbtList
    }

    fun getDebugData(): List<SettlementDebugData> {
        val data = mutableListOf<SettlementDebugData>()
        this.structures.forEach { structure ->
            data.add(
                SettlementDebugData(
                    structure.key,
                    structure.value.capacity,
                    structure.value.MAX_CAPACITY,
                    structure.value.region.lower,
                    structure.value.region.upper,
                ),
            )
        }
        return data
    }

    companion object {
        // TODO: find a better impl to get Map ids
        fun getAvailableKey(existingNumbers: List<Int>): Int {
            var idNumber: Int = 0
            do {
                idNumber = ++idNumber
            } while (idNumber in existingNumbers)
            return idNumber
        }

        fun alliesDeserialize(nbtList: NbtList): ArrayList<PlayerDataField> {
            val alliesList = arrayListOf<PlayerDataField>()
            for (i in 0 until nbtList.size) {
                val data = nbtList.getCompound(i)
                alliesList.add(PlayerDataField(data.getInt("AllyReputation"), data.getUuid("AllyUUID")))
            }
            return alliesList
        }

        fun structuresDeserialize(nbtList: NbtList): MutableMap<Int, Structure> {
            val structureList = mutableMapOf<Int, Structure>()
            for (i in 0 until nbtList.size) {
                val nbt = nbtList.getCompound(i)
                val lower =
                    BlockPos(nbt.getInt("StructureAreaLowerX"), nbt.getInt("StructureAreaLowerY"), nbt.getInt("StructureAreaLowerZ"))
                val upper =
                    BlockPos(nbt.getInt("StructureAreaUpperX"), nbt.getInt("StructureAreaUpperY"), nbt.getInt("StructureAreaUpperZ"))
                when (nbt.getInt("StructureType")) {
                    StructureType.KITCHEN.ordinal -> {
                        Building(
                            StructureType.KITCHEN,
                            lower,
                            upper,
                            nbt.getInt("StructureCapacity"),
                        )
                    }
                    StructureType.HOUSE.ordinal -> {
                        Building(
                            StructureType.HOUSE,
                            lower,
                            upper,
                            nbt.getInt("StructureCapacity"),
                        )
                    }
                    StructureType.FARM.ordinal -> {
                        Farm(
                            lower,
                            upper,
                            nbt.getInt("StructureCapacity"),
                        )
                    }
                    StructureType.POND.ordinal -> {
                        Pond(lower, upper)
                    }
                    else -> null
                }?.let {
                    structureList[nbt.getInt("StructureKey")] = it
                    nbt.getIntArray("StructureSettlers").forEach { vid ->
                        it.addResident(vid)
                    }
                }
            }
            return structureList
        }

        fun fromNbt(nbt: NbtCompound): Settlement {
            val id = nbt.getInt("VillageKey")
            val name = nbt.getString("VillageName")
            val pos = BlockPos(nbt.getInt("SettlementOriginPosX"), nbt.getInt("SettlementOriginPosY"), nbt.getInt("SettlementOriginPosZ"))
            val dim = nbt.getString("DimensionType")
            val settlers = nbt.getIntArray("VillagersData").map { Settler(it, null) }.toMutableList()
            val structures = structuresDeserialize(nbt.getList("StructuresData", NbtElement.COMPOUND_TYPE.toInt()))
            val allies = alliesDeserialize(nbt.getList("AlliesData", NbtElement.COMPOUND_TYPE.toInt()))
            return Settlement(id, name, pos, dim, structures, settlers, allies)
        }
    }
}
